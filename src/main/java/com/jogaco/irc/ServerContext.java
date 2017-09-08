package com.jogaco.irc;

import com.jogaco.irc.IRCServer.Chat;

public interface ServerContext {
    public final int MAX_CLIENTS_PER_CHANNEL = 10;
    public final int MAX_MESSAGES = 20;
    

    public void handleCommand(ClientContext clientContext, String command) throws IRCException;

    public Chat getOrCreateChat(String param);
}