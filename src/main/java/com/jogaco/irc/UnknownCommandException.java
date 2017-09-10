package com.jogaco.irc;

class UnknownCommandException extends IRCException {
    public final String UNKNOWN_COMMAND = "Unknown Command Error:";

    private final String command;

    UnknownCommandException(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public String getMessage() {
        StringBuilder builder = new StringBuilder(UNKNOWN_COMMAND);
        builder.append(getCommand());
        return builder.toString();
    }
}
