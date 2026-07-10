package com.dbp.uripet.config.error;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends ClientErrorException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, HttpStatus.CONFLICT.getReasonPhrase(), message);
    }
}