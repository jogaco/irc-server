package com.jogaco.irc;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.string.StringDecoder;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

public class IRCServerHandlerTest {
    @Test
    public void nettyTest() {
        EmbeddedChannel channel = new EmbeddedChannel(new StringDecoder(StandardCharsets.UTF_8));
        channel.writeInbound("echo");
        String myObject = channel.readInbound();
        // Perform checks on your object
        assertEquals("echo", myObject);
    }
}
