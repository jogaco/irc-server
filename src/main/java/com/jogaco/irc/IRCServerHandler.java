package com.jogaco.irc;

import io.netty.buffer.ByteBuf;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Handles a server-side channel.
 */
public class IRCServerHandler extends ChannelInboundHandlerAdapter implements ClientContext {
    private User user;
    private ServerContext serverContext;
    
    public IRCServerHandler(ServerContext context) {
        serverContext = context;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // Discard the received data silently.
        ctx.channel();
        ByteBuf in = (ByteBuf) msg;
        String command = in.toString(io.netty.util.CharsetUtil.US_ASCII);
        serverContext.handleCommand(this, command);
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
}