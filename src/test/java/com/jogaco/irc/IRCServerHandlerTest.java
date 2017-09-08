package com.jogaco.irc;

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
    }
}
