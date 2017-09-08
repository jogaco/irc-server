package com.jogaco.irc;

public interface ClientContext {
    
    void setUser(User user);
    
    User getUser();

    public void setOutput(String output);
}
