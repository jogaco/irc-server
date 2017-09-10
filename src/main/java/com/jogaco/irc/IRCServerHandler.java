package com.jogaco.irc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles a server-side channel. One instance per client connection
 */
public class IRCServerHandler extends ChannelInboundHandlerAdapter implements ClientContext {
    private User user;
    private ServerContext serverContext;
    private String output;
    private IRCServer.Chat channel;
    private Channel netChannel;
    
    public IRCServerHandler(ServerContext context) {
        serverContext = context;
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        netChannel = ctx.channel();
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (channel != null) {
            channel.leave(this);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf in = (ByteBuf) msg;
        String command = in.toString(io.netty.util.CharsetUtil.US_ASCII);
        String response = null;
        try {
            serverContext.handleCommand(this, command);
            if (this.output != null) {
                response = output;
            }
        } catch (UnknownCommandError ex) {
            // Logger.getLogger(IRCServerHandler.class.getName()).log(Level.SEVERE, null, ex);
            response = "Unknown Command Error:" + ex.getCommand();
            
        } catch (LoginRequiredError ex) {
            response = PLEASE_LOG_IN;
        } catch (ErrorInCommandException | UserWrongPasswordException ex) {
            response = ex.getMessage();
        } catch (ChannelMaxUsersException ex) {
            response = ex.getMessage();
        } catch (IRCException ex) {
            Logger.getLogger(IRCServerHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (response != null) {
            StringBuilder builder = new StringBuilder(response.length() + 1);
            builder.append(response);
//            if (!response.endsWith(System.lineSeparator()) && !response.isEmpty()) {
//                builder.append(System.lineSeparator());
//            }
            ctx.write(Unpooled.copiedBuffer(builder.toString().getBytes()));
            ctx.flush();
        }
        ((ByteBuf) msg).release();
    }
    static final String PLEASE_LOG_IN = "Please log in with /login user passwd";

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
    public void setCurrentChannel(IRCServer.Chat channel) {
        if (this.channel != null) {
            this.channel.leave(this);
        }

        this.channel = channel;
    }

    @Override
    public IRCServer.Chat getCurrentChannel() {
        return channel;
    }

    @Override
    public void setOutput(String output) {
        this.output = output;
    }
    
    @Override
    public void notify(UserMessage msg) {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(msg.getFormattedMessage().getBytes());
        netChannel.writeAndFlush(buf);
   }
}