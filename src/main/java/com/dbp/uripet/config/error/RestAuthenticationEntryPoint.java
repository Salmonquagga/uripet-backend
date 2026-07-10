package com.dbp.uripet.config.error;

import com.dbp.uripet.config.error.dto.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
        writeError(response, request, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", ex.getMessage() != null ? ex.getMessage() : "Unauthorized");
    }

    private void writeError(HttpServletResponse response, HttpServletRequest request, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), new ErrorResponseDTO(
                OffsetDateTime.now(),
                status,
                error,
                message,
                request.getRequestURI()
        ));
    }
}
