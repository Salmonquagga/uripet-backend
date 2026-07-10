package com.dbp.uripet.config.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
    }

    @Test
    void doFilter_whenNotQrPath_shouldProceed() throws Exception {
        when(request.getRequestURI()).thenReturn("/pets/me");
        when(request.getContextPath()).thenReturn("");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilter_whenQrPathAndUnderLimit_shouldProceed() throws Exception {
        when(request.getRequestURI()).thenReturn("/pets/some-uuid/qr-data");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void doFilter_whenQrPathAndExceedsLimit_shouldReturn429() throws Exception {
        when(request.getRequestURI()).thenReturn("/pets/some-uuid/qr-data");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        // Max requests limit is 30. Send 30 requests.
        for (int i = 0; i < 30; i++) {
            filter.doFilter(request, response, chain);
        }

        // The 31st request should be blocked.
        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        assertEquals("Too many requests. Please try again later.", stringWriter.toString().trim());
        // Verify filter chain was only invoked 30 times, not 31
        verify(chain, times(30)).doFilter(request, response);
    }
}
