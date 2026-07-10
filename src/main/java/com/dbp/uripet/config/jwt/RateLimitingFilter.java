package com.dbp.uripet.config.jwt;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class RateLimitingFilter implements Filter {

    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private final ConcurrentHashMap<String, Queue<Instant>> requestTimesByIp = new ConcurrentHashMap<>();
    private volatile long lastCleanupTime = System.currentTimeMillis();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();
        String targetPath = (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) 
                ? path.substring(contextPath.length()) 
                : path;

        if (targetPath.matches("^/pets/[^/]+/qr-data$")) {
            performPeriodicCleanup();
            String ip = getClientIp(httpRequest);
            Instant now = Instant.now();

            Queue<Instant> times = requestTimesByIp.computeIfAbsent(ip, k -> new ConcurrentLinkedQueue<>());
            synchronized (times) {
                Instant oneMinuteAgo = now.minusSeconds(60);
                while (!times.isEmpty() && times.peek().isBefore(oneMinuteAgo)) {
                    times.poll();
                }

                if (times.size() >= MAX_REQUESTS_PER_MINUTE) {
                    httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    httpResponse.setContentType("text/plain");
                    httpResponse.getWriter().write("Too many requests. Please try again later.");
                    return;
                }

                times.add(now);
            }
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    private void performPeriodicCleanup() {
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastCleanupTime > 60000) {
            lastCleanupTime = nowMs;
            Instant oneMinuteAgo = Instant.now().minusSeconds(60);
            requestTimesByIp.forEach((ip, times) -> {
                synchronized (times) {
                    while (!times.isEmpty() && times.peek().isBefore(oneMinuteAgo)) {
                        times.poll();
                    }
                    if (times.isEmpty()) {
                        requestTimesByIp.remove(ip);
                    }
                }
            });
        }
    }
}
