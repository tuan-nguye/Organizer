package com.org.parser.command;

/**
 * Command exception are thrown when a validating or executing a command
 * leads to an error.
 */
public class CommandException extends Exception {
    public CommandException(String message) {
        super(message);
    }
}
