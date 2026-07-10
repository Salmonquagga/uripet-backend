package com.dbp.uripet.config.error;

import com.dbp.uripet.config.error.dto.ErrorResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void handleNotFound_shouldReturnConsistentErrorResponse() {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/pets/abc"
                );

        var response =
                handler.handleNotFound(
                        new ResourceNotFoundException(
                                "Pet not found"
                        ),
                        request
                );

        ErrorResponseDTO body =
                response.getBody();

        assertEquals(
                HttpStatus.NOT_FOUND,
                response.getStatusCode()
        );

        assertNotNull(body);
        assertNotNull(body.timestamp());

        assertEquals(
                404,
                body.status()
        );

        assertEquals(
                "Not Found",
                body.error()
        );

        assertEquals(
                "Pet not found",
                body.message()
        );

        assertEquals(
                "/pets/abc",
                body.path()
        );
    }

    @Test
    void handleMethodArgumentNotValid_shouldAggregateFieldErrors()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "POST",
                        "/auth/register"
                );

        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(
                        new Object(),
                        "registerRequest"
                );

        bindingResult.addError(
                new FieldError(
                        "registerRequest",
                        "email",
                        "must not be blank"
                )
        );

        bindingResult.addError(
                new FieldError(
                        "registerRequest",
                        "password",
                        "must not be blank"
                )
        );

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        null,
                        bindingResult
                );

        var response =
                handler.handleMethodArgumentNotValid(
                        exception,
                        request
                );

        ErrorResponseDTO body =
                response.getBody();

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(body);

        assertEquals(
                400,
                body.status()
        );

        assertEquals(
                "Validation Error",
                body.error()
        );

        assertEquals(
                "email: must not be blank; "
                        + "password: must not be blank",
                body.message()
        );

        assertEquals(
                "/auth/register",
                body.path()
        );
    }

    @Test
    void handleHttpMessageNotReadable_shouldReturnBadRequest() {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "POST",
                        "/auth/login"
                );

        HttpMessageNotReadableException exception =
                new HttpMessageNotReadableException(
                        "Malformed JSON",
                        new IllegalArgumentException(
                                "Unexpected token"
                        ),
                        null
                );

        var response =
                handler.handleHttpMessageNotReadable(
                        exception,
                        request
                );

        ErrorResponseDTO body =
                response.getBody();

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );

        assertNotNull(body);

        assertEquals(
                400,
                body.status()
        );

        assertEquals(
                "Bad Request",
                body.error()
        );

        assertEquals(
                "Malformed request body or invalid field value",
                body.message()
        );

        assertEquals(
                "/auth/login",
                body.path()
        );
    }
}