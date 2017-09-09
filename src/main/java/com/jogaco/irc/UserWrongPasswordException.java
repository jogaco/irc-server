package com.jogaco.irc;

public class UserWrongPasswordException extends IRCException {

    public UserWrongPasswordException() {
    }

    public UserWrongPasswordException(String msg) {
        super(msg);
    }

}
