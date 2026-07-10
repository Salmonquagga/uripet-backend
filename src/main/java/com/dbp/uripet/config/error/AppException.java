package com.dbp.uripet.config.error;

import org.springframework.http.HttpStatus;

public abstract class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    protected AppException(HttpStatus status, String error, String message) {
        super(message);
        this.status = status;
        this.error = error;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}