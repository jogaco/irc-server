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
    public void logout(User user) {
        synchronized (users) {
            users.remove(user);
        }
        Chat userChannel = user.getCurrentChannel();
        if (userChannel != null) {
            userChannel.leave(user);
        }
    }
    
    class CommandDecoder {
        
        Command createCommand(String command) {
            if (command != null) {
                if (command.startsWith("/login ") || command.equals("/login")) {
                    return new LoginCommand();
                } else if (command.startsWith("/join ") || command.equals("/join")) {
                    return new ChannelCommand();
                } else if (command.startsWith("/leave ") || command.equals("/leave")) {
                    return new LogoutCommand();
                }
            }
            return null;
        }
    }
    
    interface Command {
        
        void run(ClientContext clientContext, ServerContext serverContext, String command) throws IRCException;
    }
    
    class LoginCommand implements Command {
        
        static final String MISSING_PARAMS = "Error: /login user passwd";
        static final String SUCCESS = "Welcome";
        static final String WRONG_PASSWD = "Error: incorrect password";

        @Override
        public void run(ClientContext clientContext, ServerContext serverContext, String command) throws IRCException {
            String[] params = command.split(" ");
            if (params.length == 3) {
                User user = new User(params[1], params[2]);
                try {
                    serverContext.loginOrCreateUser(user);
                } catch (UserWrongPasswordException ex) {
                    throw new UserWrongPasswordException(WRONG_PASSWD);
                }
                clientContext.setUser(user);
                clientContext.setOutput(SUCCESS);
            } else {
                throw new ErrorInCommandException(MISSING_PARAMS);
            }
        }
    }
    
    class LogoutCommand implements Command {
        static final String SUCCESS = "Goodbye";

        @Override
        public void run(ClientContext clientContext, ServerContext serverContext, String command) throws IRCException {
            serverContext.logout(clientContext.getUser());
            clientContext.setOutput(SUCCESS);
        }
        
    }
    
    class ChannelCommand implements Command {
        static final String MISSING_PARAMS = "Error: /channel channel_name";

        @Override
        public void run(ClientContext clientContext, ServerContext serverContext, String command) throws IRCException {
            if (clientContext.getUser() == null) {
                throw new LoginRequiredError();
            }
            String[] params = command.split(" ");
            if (params.length == 2) {
                Chat chat = serverContext.getOrCreateChat(params[1]);
                chat.join(clientContext.getUser());
                final List<UserMessage> messages = chat.getMessages();
                StringBuilder builder = new StringBuilder();
                for (UserMessage usrMsg : messages) {
                    builder.append(usrMsg.getUser());
                    builder.append(": ");
                    builder.append(usrMsg.getMessage());
                    builder.append("\n");
                }
                clientContext.setOutput(builder.toString());
                
            } else {
                throw new ErrorInCommandException(MISSING_PARAMS);
            }
        }
        
    }
    
    class Chat {
        private String name;
        private Set<User> users;
        private List<UserMessage> messages;
        
        Chat(String name) {
            this.name = name;
            users = new LinkedHashSet<>(ServerContext.MAX_CLIENTS_PER_CHANNEL);
            messages = new LimitedSizeQueue<>(ServerContext.MAX_MESSAGES);
        }

        void join(User user) throws ChannelMaxUsersException {
            synchronized (users) {
                if (users.contains(user)) {
                    return;
                }
                if (users.size() == ServerContext.MAX_CLIENTS_PER_CHANNEL) {
                    throw new ChannelMaxUsersException();
                }
            }
        }

        public List<UserMessage> getMessages() {
            return messages;
        }

        void leave(User user) {
            synchronized (users) {
                users.remove(user);
            }
        }

        List<User> getUsers() {
            return new ArrayList<>(users);
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
        Command cmd = commandDecoder.createCommand(command);
        if (cmd != null) {
            cmd.run(clientContext, this, command);
        } else {
            throw new UnknownCommandError(command);
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