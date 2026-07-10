package com.dbp.uripet.config.error;

import com.dbp.uripet.config.error.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Not Found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponseDTO> handleAppException(
            AppException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                exception.getStatus(),
                exception.getError(),
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(this::formatFieldError)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Validation failed";
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                message,
                request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        String message =
                exception.getConstraintViolations()
                        .stream()
                        .map(violation ->
                                violation.getPropertyPath()
                                        + ": "
                                        + violation.getMessage()
                        )
                        .distinct()
                        .collect(Collectors.joining("; "));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation Error",
                message,
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Malformed request body or invalid field value",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String expectedType =
                exception.getRequiredType() != null
                        ? exception.getRequiredType().getSimpleName()
                        : "valid value";

        String message =
                "Invalid value for parameter '"
                        + exception.getName()
                        + "'. Expected "
                        + expectedType;

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                message,
                request
        );
    }

    @ExceptionHandler(MissingRequestValueException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingRequestValue(
            MissingRequestValueException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "A required request value is missing",
                request
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method Not Allowed",
                "HTTP method is not supported for this endpoint",
                request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthentication(
            AuthenticationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Authentication is required or the credentials are invalid",
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "You do not have permission to perform this action",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "Database integrity conflict at {}",
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                HttpStatus.CONFLICT,
                "Conflict",
                "The operation conflicts with existing data",
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                hasText(exception.getMessage())
                        ? exception.getMessage()
                        : "Invalid request value",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneral(
            Exception exception,
            HttpServletRequest request
    ) {
        log.error(
                "Unexpected error processing {} {}",
                request.getMethod(),
                request.getRequestURI(),
                exception
        );

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred",
                request
        );
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request
    ) {
        ErrorResponseDTO body =
                new ErrorResponseDTO(
                        OffsetDateTime.now(),
                        status.value(),
                        hasText(error)
                                ? error
                                : status.getReasonPhrase(),
                        hasText(message)
                                ? message
                                : status.getReasonPhrase(),
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(status)
                .body(body);
    }

    private String formatFieldError(
            FieldError fieldError
    ) {
        if (fieldError == null) {
            return null;
        }

        String message =
                fieldError.getDefaultMessage();

        if (!hasText(message)) {
            message = "Invalid value";
        }

        return fieldError.getField()
                + ": "
                + message;
    }

    private boolean hasText(
            String value
    ) {
        return value != null
                && !value.isBlank();
    }
}