package com.jogaco.irc;

class LoginRequiredException extends IRCException {
    static final String PLEASE_LOG_IN = "Please log in with /login user passwd\n";

    public LoginRequiredException() {
        super(PLEASE_LOG_IN);
    }
}
