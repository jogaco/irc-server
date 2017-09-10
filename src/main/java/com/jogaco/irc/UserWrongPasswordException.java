package com.jogaco.irc;

public class UserWrongPasswordException extends IRCException {
    static final String WRONG_PASSWD = "Error: incorrect password\n";

    public UserWrongPasswordException() {
        super(WRONG_PASSWD);
    }
}
