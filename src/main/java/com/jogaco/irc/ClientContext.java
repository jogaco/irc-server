package com.jogaco.irc;

public interface ClientContext {
    
    void setUser(User user);
    
    User getUser();

    public void setOutput(String output);
    
    void notify(UserMessage msg);
    
    public void setCurrentChannel(IRCServer.Chat channel);

    public IRCServer.Chat getCurrentChannel();

}
