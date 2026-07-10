package com.dbp.uripet.config.error;

import org.springframework.http.HttpStatus;

public class ServerErrorException extends AppException {

    public ServerErrorException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), message);
    }

    public ServerErrorException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), message);
        initCause(cause);
    }
}
