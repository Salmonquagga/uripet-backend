package com.dbp.uripet.config.error.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

public record ErrorResponseDTO(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}