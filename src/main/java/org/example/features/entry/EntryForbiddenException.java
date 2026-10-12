package org.example.features.entry;

public class EntryForbiddenException extends RuntimeException {
    public EntryForbiddenException(String message) {
        super(message);
    }
}
