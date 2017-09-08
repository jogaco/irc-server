package com.jogaco.irc;

import java.util.Objects;


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

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + Objects.hashCode(this.username);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final User other = (User) obj;
        if (!Objects.equals(this.username, other.username)) {
            return false;
        }
        return true;
    }
    
}

