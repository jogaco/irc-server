package com.jogaco.irc;

import io.netty.bootstrap.ServerBootstrap;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

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
    
    class CommandDecoder {
        
        Command createCommand(String command) {
            if (command != null) {
                if (command.startsWith("/login ")) {
                    return new LoginCommand();
                }
            }
            return null;
        }
    }
    
    interface Command {
        
        void run(ClientContext clientContext, ServerContext serverContext, String command) throws ErrorInCommandException;
    }
    
    class LoginCommand implements Command {
        static final String MISSING_PARAMS = "Error: /login user passwd";

        @Override
        public void run(ClientContext clientContext, ServerContext serverContext, String command) throws ErrorInCommandException {
            String[] params = command.split(" ");
            if (params.length == 3) {
                User user = new User(params[1], params[2]);
                clientContext.setUser(user);
                clientContext.setOutput("Welcome");
            } else {
                throw new ErrorInCommandException(MISSING_PARAMS);
            }
        }
    }
    
    public IRCServer(int port) {
        this.port = port;
        this.commandDecoder = new CommandDecoder();
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
    public void handleCommand(ClientContext clientContext, String command) throws UnknownCommandError, ErrorInCommandException {
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