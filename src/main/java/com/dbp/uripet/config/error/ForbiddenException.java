package com.dbp.uripet.config.error;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends AppException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.getReasonPhrase(), message);
    }
}
