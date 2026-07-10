package com.dbp.uripet.config.error;

import org.springframework.http.HttpStatus;

public abstract class NotFoundException extends ClientErrorException {

    protected NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), message);
    }
}