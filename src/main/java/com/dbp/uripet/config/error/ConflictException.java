package com.dbp.uripet.config.error;

public class ConflictException extends DuplicateResourceException {
    public ConflictException(String message) {
        super(message);
    }
}
