package com.jogaco.irc;


public class User {
    private final String username;
    private final String passwd;

    public User(String username, String passwd) {
        this.username = username;
        this.passwd = passwd;
    }
    
    public String getUsername() {
        return username;
    }
}

