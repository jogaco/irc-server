package com.jogaco.irc;

import io.netty.bootstrap.ServerBootstrap;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
IRCServer

Client: telnet
Client Commands:

/login name password — if user not exists create profile else login
/join channel —try to join channel (max 10 active clients per channel is needed)
    If client’s limit exceeded - send error, otherwise join channel and send last N messages of activity
/leave - disconnect client
/users — show users in the channel
text message terminated with CR - sends message to current channel.
    Server must send new message to all connected to this channel clients.
*/
public class IRCServer implements ServerContext {
    
    private int port;
    private final CommandDecoder commandDecoder;
    private final Map<String, Chat> chats;
    private final Map<String, User> users;

    @Override
    public Chat getOrCreateChat(String name) {
        Chat theChat = null;
        synchronized (chats) {
            theChat = chats.get(name);
            if (theChat == null) {
                theChat = new Chat(name);
                chats.put(name, theChat);
            }
        }
        return theChat;
    }

    @Override
    public void loginOrCreateUser(User user) throws UserWrongPasswordException {
        User theUser = null;
        synchronized (users) {
            theUser = users.get(user.getUsername());
            if (theUser == null) {
                theUser = user;
                users.put(user.getUsername(), theUser);
            }
        }
        theUser.verifyPasswd(user);
    }

    @Override
    public boolean logout(ClientContext client) {
        User user = client.getUser();
        if (user != null) {
            synchronized (users) {
                users.remove(user.getUsername());
            }
            Chat userChannel = client.getCurrentChannel();
            if (userChannel != null) {
                userChannel.leave(client);
            }
        }
        return user != null;
    }

    class CommandDecoder {
        // Stateless: build upfront
        private final Command LOGIN_COMMAND = new LoginCommand();
        private final Command CHANNEL_COMMAND = new ChannelCommand();
        private final Command LOGOUT_COMMAND = new LogoutCommand();
        private final Command USERS_COMMAND = new UsersCommand();
        private final Command MESSAGE_COMMAND = new MessageCommand();
        
        Command getCommand(String command) {
            if (command != null && !command.isEmpty()) {
                if (command.startsWith("/login ") || command.equals("/login")) {
                    return LOGIN_COMMAND;
                } else if (command.startsWith("/join ") || command.equals("/join")) {
                    return CHANNEL_COMMAND;
                } else if (command.equals("/leave")) {
                    return LOGOUT_COMMAND;
                } else if (command.equals("/users")) {
                    return USERS_COMMAND;
                } else {
                    return MESSAGE_COMMAND;
                }
            }
            return null;
        }
    }
    
    interface Command {
        
        void run(ClientContext clientContext, ServerContext serverContext, String command) throws IRCException;
    }
    
    class LoginCommand implements Command {
        
        static final String MISSING_PARAMS = "Error: /login user passwd\n";
        static final String SUCCESS = "Welcome\n";

        @Override
        public void run(ClientContext clientContext, ServerContext serverContext, String command) throws IRCException {
            String[] params = command.split(" ");
            if (params.length == 3) {
                User user = new User(params[1], params[2]);
                try {
                    serverContext.loginOrCreateUser(user);
                } catch (UserWrongPasswordException ex) {
                    throw new UserWrongPasswordException();
                }
                clientContext.setUser(user);
                clientContext.setOutput(SUCCESS);
            } else {
                throw new ErrorInCommandException(MISSING_PARAMS);
            }
        }
    }
    
    class LogoutCommand implements Command {
        static final String SUCCESS = "Goodbye\n";

        @Override
        public void run(ClientContext clientContext, ServerContext serverContext, String command) throws IRCException {
            serverContext.logout(clientContext);
            clientContext.setOutput(SUCCESS);
        }
        
    }
    
    class ChannelCommand implements Command {
        static final String MISSING_PARAMS = "Error: /join channel_name\n";

        @Override
        public void run(ClientContext clientContext, ServerContext serverContext, String command) throws IRCException {
            if (clientContext.getUser() == null) {
                throw new LoginRequiredException();
            }
            String[] params = command.split(" ");
            if (params.length == 2) {
                Chat chat = serverContext.getOrCreateChat(params[1]);
                chat.join(clientContext);

                final List<UserMessage> messages = chat.getMessages();
                StringBuilder builder = new StringBuilder();
                for (UserMessage usrMsg : messages) {
                    builder.append(usrMsg.getFormattedMessage());
                }
                clientContext.setOutput(builder.toString());
                
            } else {
                throw new ErrorInCommandException(MISSING_PARAMS);
            }
        }
        
    }
    
    class UsersCommand implements Command {

        @Override
        public void run(ClientContext clientContext, ServerContext serverContext, String command) throws IRCException {
            Chat chat = clientContext.getCurrentChannel();
            StringBuilder builder = new StringBuilder();
            if (chat != null) {
                final List<User> users = chat.getUsers();
                for (User usr : users) {
                    builder.append(usr.getUsername());
                    builder.append(System.lineSeparator());
                }
            }
            clientContext.setOutput(builder.toString());
        }
        
    }
    
    class MessageCommand implements Command {

        @Override
        public void run(ClientContext clientContext, ServerContext serverContext, String command) throws IRCException {
            Chat chat = clientContext.getCurrentChannel();
            if (chat != null) {
                chat.sendMessage(command, clientContext);
            }
            clientContext.setOutput("");
        }
        
    }
    
    class Chat {
        final private String name;
        final private Set<User> users;
        final private List<UserMessage> messages;
        final private Set<ClientContext> clients;
        
        Chat(String name) {
            this.name = name;
            users = new LinkedHashSet<>(ServerContext.MAX_CLIENTS_PER_CHANNEL);
            messages = new LimitedSizeQueue<>(ServerContext.MAX_MESSAGES);
            clients = new LinkedHashSet<>(ServerContext.MAX_CLIENTS_PER_CHANNEL);
        }

        int maxClientsPerChannel() {
            return ServerContext.MAX_CLIENTS_PER_CHANNEL;
        }

        void join(ClientContext client) throws ChannelMaxUsersException {
            User user = client.getUser();

            synchronized (users) {
                if (users.contains(user)) {
                    return;
                }
                if (users.size() == maxClientsPerChannel()) {
                    throw new ChannelMaxUsersException();
                }
                users.add(user);
                clients.add(client);
            }

            client.setCurrentChannel(this);
        }

        synchronized public List<UserMessage> getMessages() {
            return messages;
        }

        void leave(ClientContext client) {
            User user = client.getUser();
            synchronized (users) {
                users.remove(user);
                clients.remove(client);
            }
        }

        List<User> getUsers() {
            return new ArrayList<>(users);
        }

        void sendMessage(String command, ClientContext clientContext) {
            if (clientContext.getUser() != null) {
                UserMessage userMsg = new UserMessage(clientContext.getUser(), command);

                synchronized (messages) {
                    messages.add(userMsg);
                }

                synchronized (clients) {
                    for (ClientContext otherClient : clients) {
                        if (clientContext != otherClient) {
                            otherClient.notify(userMsg);
                        }
                    }
                }
            }
        }
    }

    public IRCServer(int port) {
        this.port = port;
        this.commandDecoder = new CommandDecoder();
        chats = new HashMap<>();
        users = new HashMap<>();
    }
    
    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ch.pipeline().addLast(new IRCServerHandler(IRCServer.this));
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);
    
            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync();
    
            // Wait until the server socket is closed.
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    @Override
    public void handleCommand(ClientContext clientContext, String command) throws IRCException {
        command = command.trim();
        Command cmd = commandDecoder.getCommand(command);
        if (cmd != null) {
            cmd.run(clientContext, this, command);
        } else if (!command.trim().isEmpty()) {
            throw new UnknownCommandException(command);
        }
    }
    
    public static void main(String[] args) throws Exception {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }
        new IRCServer(port).run();
    }
}