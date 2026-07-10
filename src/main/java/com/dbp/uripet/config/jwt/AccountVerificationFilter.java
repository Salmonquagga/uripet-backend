package com.dbp.uripet.config.jwt;

import com.dbp.uripet.config.error.ForbiddenException;
import com.dbp.uripet.config.error.UnauthorizedException;
import com.dbp.uripet.config.error.dto.ErrorResponseDTO;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class AccountVerificationFilter extends OncePerRequestFilter {
    private final UserService userService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (isWhitelisted(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            User user = userService.getAuthenticatedUser();
            if (!user.isVerified()) {
                writeError(request, response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", "Account not verified");
                return;
            }
        } catch (ForbiddenException ex) {
            writeError(request, response, HttpServletResponse.SC_FORBIDDEN, "Forbidden", ex.getMessage());
            return;
        } catch (UnauthorizedException ex) {
            writeError(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", ex.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String path) {
        return path.equals("/auth/login")
                || path.equals("/auth/register")
                || path.equals("/auth/verify")
                || path.equals("/auth/resend-verification")
                || path.matches("^/pets/public/[^/]+$")
                || path.matches("^/pets/[^/]+/qr-data$")
                || path.equals("/billing/webhook")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/api/v3/api-docs/")
                || path.equals("/api/docs")
                || path.startsWith("/api/docs/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/api/swagger-ui/")
                || path.equals("/swagger-ui.html")
                || path.equals("/api/swagger-ui.html");
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String error,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), new ErrorResponseDTO(
                OffsetDateTime.now(),
                status,
                error,
                message != null ? message : "Unexpected error",
                request.getRequestURI()
        ));
    }
}
