package com.jogaco.irc;

public class ChannelMaxUsersException extends IRCException {
    static final String TOO_MANY_USERS = "Too many users";

    public ChannelMaxUsersException() {
        super(TOO_MANY_USERS);
    }
    

}
