package com.jogaco.irc;

import com.jogaco.irc.IRCServer.LoginCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class IRCServerHandlerTest {
    
    @Test
    public void handleLogin() {
        ServerContext serverContext = new IRCServer(1);
        
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.writeInbound(Unpooled.wrappedBuffer("/login user user".getBytes()));
        
        assertThat(handler.getUser().getUsername(), is("user"));
        
        ByteBuf buf = channel.readOutbound();
        String response = buf.toString(io.netty.util.CharsetUtil.US_ASCII);

        assertThat(response, is("Welcome"));
    }
    
    @Test
    public void handleLoginMissingParams() {
        ServerContext serverContext = new IRCServer(1);
        
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.writeInbound(Unpooled.wrappedBuffer("/login user".getBytes()));
        
        ByteBuf buf = channel.readOutbound();
        String response = buf.toString(io.netty.util.CharsetUtil.US_ASCII);

        assertThat(response, is(LoginCommand.MISSING_PARAMS));
    }
}
