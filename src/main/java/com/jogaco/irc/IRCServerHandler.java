package com.jogaco.irc;

import io.netty.buffer.ByteBuf;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles a server-side channel.
 */
public class IRCServerHandler extends ChannelInboundHandlerAdapter implements ClientContext {
    private User user;
    private ServerContext serverContext;
    private String output;
    
    public IRCServerHandler(ServerContext context) {
        serverContext = context;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        String command = in.toString(io.netty.util.CharsetUtil.US_ASCII);
        try {
            serverContext.handleCommand(this, command);
        } catch (UnknownCommandError ex) {
            // Logger.getLogger(IRCServerHandler.class.getName()).log(Level.SEVERE, null, ex);
            ctx.write("Unknown Command Error:" + ex.getCommand());
            
        } catch (LoginRequiredError ex) {
            ctx.write("Please log in with /login user passwd");
        }
        if (this.output != null) {
            ctx.write(this.output);
        }
        ctx.flush();
        ((ByteBuf) msg).release();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setOutput(String output) {
        this.output = output;
    }
}