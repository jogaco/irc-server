package com.jogaco.irc;

public class UserMessage {

    private final User user;
    private final String message;
    
    public UserMessage(User user, String msg) {
        this.user = user;
        this.message = msg;
    }

    public String getUsername() {
        return user.getUsername();
    }
    public User getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }
    
}
