package com.dbp.uripet.config.error;

import org.springframework.http.HttpStatus;

public class ValidationException extends ClientErrorException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.getReasonPhrase(), message);
    }
}