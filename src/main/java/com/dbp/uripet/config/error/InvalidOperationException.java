package com.dbp.uripet.config.error;

import org.springframework.http.HttpStatus;

public class InvalidOperationException extends ClientErrorException {

    public InvalidOperationException(String message) {
        super(HttpStatus.CONFLICT, HttpStatus.CONFLICT.getReasonPhrase(), message);
    }
}