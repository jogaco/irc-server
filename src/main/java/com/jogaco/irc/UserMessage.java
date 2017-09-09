package com.jogaco.irc;

public class UserMessage {

    private final User user;
    private final String message;
    private String formattedMessage;
    
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
    
    public String getFormattedMessage() {
        if (formattedMessage == null) {
            StringBuilder builder = new StringBuilder(getMessage().length() + getUsername().length() + 2);
            builder.append(getUsername());
            builder.append(": ");
            builder.append(getMessage());
            formattedMessage = builder.toString();
        }
        return formattedMessage;
    }
}
