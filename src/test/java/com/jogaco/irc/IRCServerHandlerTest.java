package com.jogaco.irc;

import com.jogaco.irc.IRCServer.ChannelCommand;
import com.jogaco.irc.IRCServer.LoginCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

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

        assertThat(response, is(LoginCommand.SUCCESS));
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
    
    @Test
    public void handleJoinChannelNoLogin() {
        ServerContext serverContext = new IRCServer(1);
        
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.writeInbound(Unpooled.wrappedBuffer("/join channel".getBytes()));
        
        ByteBuf buf = channel.readOutbound();
        String response = buf.toString(io.netty.util.CharsetUtil.US_ASCII);

        assertThat(response, is(IRCServerHandler.PLEASE_LOG_IN));
    }
    
    @Test
    public void handleJoinChannelMissingParam() {
        ServerContext serverContext = new IRCServer(1);
        User user = new User("user", "user");
        
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        IRCServerHandler handlerMock = spy(handler);
        when(handlerMock.getUser()).thenReturn(user);

        EmbeddedChannel channel = new EmbeddedChannel(handlerMock);
        channel.writeInbound(Unpooled.wrappedBuffer("/join".getBytes()));
        
        ByteBuf buf = channel.readOutbound();
        String response = buf.toString(io.netty.util.CharsetUtil.US_ASCII);

        assertThat(response, is(ChannelCommand.MISSING_PARAMS));
    }
    
    @Test
    public void handleJoinChannel() {
        ServerContext serverContext = new IRCServer(1);
        User user = new User("user", "user");
        
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        IRCServerHandler handlerMock = spy(handler);
        when(handlerMock.getUser()).thenReturn(user);

        EmbeddedChannel channel = new EmbeddedChannel(handlerMock);
        channel.writeInbound(Unpooled.wrappedBuffer("/join channel".getBytes()));
        
        ByteBuf buf = channel.readOutbound();
        String response = buf.toString(io.netty.util.CharsetUtil.US_ASCII);

        assertThat(response, is("")); // no messages on channel
    }
}
