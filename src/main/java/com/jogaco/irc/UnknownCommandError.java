package com.jogaco.irc;

class UnknownCommandError extends Exception {

    private final String command;

    UnknownCommandError(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

}
