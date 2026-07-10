package com.dbp.uripet.config.error;

import org.springframework.http.HttpStatus;

public abstract class ClientErrorException extends AppException {

    protected ClientErrorException(HttpStatus status, String error, String message) {
        super(status, error, message);
    }
}