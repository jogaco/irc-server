package com.jogaco.irc;

import com.jogaco.irc.IRCServer.ChannelCommand;
import com.jogaco.irc.IRCServer.Chat;
import com.jogaco.irc.IRCServer.LoginCommand;
import com.jogaco.irc.IRCServer.LogoutCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class IRCServerHandlerTest {
    
    final static String lineSep = System.lineSeparator();
    
    @Test
    public void handleLogin() {
        ServerContext serverContext = new IRCServer(1);
        
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.writeInbound(Unpooled.wrappedBuffer("/login user user".getBytes()));
        
        assertThat(handler.getUser().getUsername(), is("user"));
        
        ByteBuf buf = channel.readOutbound();
        String response = buf.toString(io.netty.util.CharsetUtil.US_ASCII);

        assertThat(response, is(LoginCommand.SUCCESS + lineSep));
    }
    
    @Test
    public void handleLoginMissingParams() {
        ServerContext serverContext = new IRCServer(1);
        
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.writeInbound(Unpooled.wrappedBuffer("/login user".getBytes()));
        
        ByteBuf buf = channel.readOutbound();
        String response = buf.toString(io.netty.util.CharsetUtil.US_ASCII);

        assertThat(response, is(LoginCommand.MISSING_PARAMS + lineSep));
    }
    
    @Test
    public void handleLoginWrongPassword() {
        ServerContext serverContext = new IRCServer(1);
        
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.writeInbound(Unpooled.wrappedBuffer("/login user user".getBytes()));
        ByteBuf buf = channel.readOutbound();
        
        channel.writeInbound(Unpooled.wrappedBuffer("/login user wrong".getBytes()));
        buf = channel.readOutbound();
        
        String response = buf.toString(io.netty.util.CharsetUtil.US_ASCII);

        assertThat(response, is(LoginCommand.WRONG_PASSWD + lineSep));
    }
    
    @Test
    public void handleJoinChannelNoLogin() {
        ServerContext serverContext = new IRCServer(1);
        
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.writeInbound(Unpooled.wrappedBuffer("/join channel".getBytes()));
        
        ByteBuf buf = channel.readOutbound();
        String response = buf.toString(io.netty.util.CharsetUtil.US_ASCII);

        assertThat(response, is(IRCServerHandler.PLEASE_LOG_IN + lineSep));
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

        assertThat(response, is(ChannelCommand.MISSING_PARAMS + lineSep));
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

        Chat chat = serverContext.getOrCreateChat("channel");
        List<User> usersInChannel = chat.getUsers();
        
        assertThat(usersInChannel.contains(user), is(true));
    }
    
    @Test
    public void handleLeaveNoChannelJoined() {
        ServerContext serverContext = new IRCServer(1);
        
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.writeInbound(Unpooled.wrappedBuffer("/login user user".getBytes()));

        channel.writeInbound(Unpooled.wrappedBuffer("/leave".getBytes()));
        
        ByteBuf buf = channel.readOutbound();
        
        buf = channel.readOutbound();
        String response = buf.toString(io.netty.util.CharsetUtil.US_ASCII);

        assertThat(response, is(LogoutCommand.SUCCESS + lineSep));
    }
    
    @Test
    public void handleLeaveChannelJoined() {
        ServerContext serverContext = new IRCServer(1);
        
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.writeInbound(Unpooled.wrappedBuffer("/login user user".getBytes()));
        User user = handler.getUser();
        channel.writeInbound(Unpooled.wrappedBuffer("/join channel".getBytes()));

        channel.writeInbound(Unpooled.wrappedBuffer("/leave".getBytes()));
        
        Chat chat = serverContext.getOrCreateChat("channel");
        List<User> usersInChannel = chat.getUsers();
        
        assertThat(usersInChannel.contains(user), is(false));
    }
    
    @Test
    public void handleJoinChannelChangeChannel() {
        ServerContext serverContext = new IRCServer(1);
        
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        channel.writeInbound(Unpooled.wrappedBuffer("/login user user".getBytes()));
        User user = handler.getUser();
        channel.writeInbound(Unpooled.wrappedBuffer("/join channel1".getBytes()));
        channel.writeInbound(Unpooled.wrappedBuffer("/join channel2".getBytes()));

        Chat chat1 = serverContext.getOrCreateChat("channel1");
        List<User> usersInChannel1 = chat1.getUsers();
        Chat chat2 = serverContext.getOrCreateChat("channel2");
        List<User> usersInChannel2 = chat2.getUsers();
        
        assertThat(usersInChannel2.contains(user), is(true));
        assertThat(usersInChannel1.contains(user), is(false));
    }

    @Test
    public void handleUsers() {
        ServerContext serverContext = new IRCServer(1);

        User user = new User("user", "user");
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        IRCServerHandler handlerMock = spy(handler);
        when(handlerMock.getUser()).thenReturn(user);
        EmbeddedChannel channel = new EmbeddedChannel(handlerMock);
        channel.writeInbound(Unpooled.wrappedBuffer("/join channel".getBytes()));
        
        User user2 = new User("user2", "user2");
        IRCServerHandler handler2 = new IRCServerHandler(serverContext);
        IRCServerHandler handlerMock2 = spy(handler2);
        when(handlerMock2.getUser()).thenReturn(user2);
        EmbeddedChannel channel2 = new EmbeddedChannel(handlerMock2);
        channel2.writeInbound(Unpooled.wrappedBuffer("/join channel".getBytes()));

        channel.writeInbound(Unpooled.wrappedBuffer("/users".getBytes()));

        ByteBuf buf = channel.readOutbound();
        
        buf = channel.readOutbound();
        String response = buf.toString(io.netty.util.CharsetUtil.US_ASCII);

        assertThat(response, is("user" + lineSep + "user2" + lineSep));
        
        Chat chat = serverContext.getOrCreateChat("channel");
        final List<User> users = chat.getUsers();
        assertThat(users, hasItems(user, user2));
    }
    @Test
    public void handleChannelMessage() {
        ServerContext serverContext = new IRCServer(1);

        User user = new User("user", "user");
        IRCServerHandler handler = new IRCServerHandler(serverContext);
        IRCServerHandler handlerMock = spy(handler);
        when(handlerMock.getUser()).thenReturn(user);
        EmbeddedChannel channel = new EmbeddedChannel(handlerMock);
        channel.writeInbound(Unpooled.wrappedBuffer("/join channel".getBytes()));
        
        User user2 = new User("user2", "user2");
        IRCServerHandler handler2 = new IRCServerHandler(serverContext);
        IRCServerHandler handlerMock2 = spy(handler2);
        when(handlerMock2.getUser()).thenReturn(user2);
        EmbeddedChannel channel2 = new EmbeddedChannel(handlerMock2);
        channel2.writeInbound(Unpooled.wrappedBuffer("/join channel".getBytes()));
        ByteBuf buf = channel2.readOutbound();

        channel.writeInbound(Unpooled.wrappedBuffer("message".getBytes()));

        buf = channel2.readOutbound();
        String response = buf.toString(io.netty.util.CharsetUtil.US_ASCII);

        assertThat(response, is("user: message"));
    }

}
