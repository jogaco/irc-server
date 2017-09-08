package com.jogaco.irc;

public interface ServerContext {

    public void handleCommand(ClientContext clientContext, String command) throws UnknownCommandError, LoginRequiredError, ErrorInCommandException;
}