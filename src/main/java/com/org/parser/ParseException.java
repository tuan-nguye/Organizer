package com.org.parser;

/**
 * Parse exceptions will be thrown if the command line parser runs into errors.
 */
public class ParseException extends Exception {
    public ParseException(String message) {
        super(message);
    }
}
