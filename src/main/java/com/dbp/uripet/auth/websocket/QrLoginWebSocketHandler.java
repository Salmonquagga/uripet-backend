package com.dbp.uripet.auth.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class QrLoginWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, Set<WebSocketSession>> listeners = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);
        if (token != null && !token.isBlank()) {
            listeners.computeIfAbsent(token, k -> new CopyOnWriteArraySet<>()).add(session);
        } else {
            // Close session if no token is provided
            session.close(CloseStatus.BAD_DATA);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String token = extractToken(session);
        if (token != null) {
            Set<WebSocketSession> sessions = listeners.get(token);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    listeners.remove(token);
                }
            }
        }
    }

    private String extractToken(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String query = uri.getQuery();
        if (query == null) return null;

        // Parse token=XYZ
        return UriComponentsBuilder.fromUri(uri)
                .build()
                .getQueryParams()
                .getFirst("token");
    }

    
    public void notifyClient(String token, Object payload) {
        Set<WebSocketSession> sessions = listeners.remove(token);
        if (sessions != null) {
            String jsonPayload;
            try {
                jsonPayload = objectMapper.writeValueAsString(payload);
            } catch (Exception e) {
                jsonPayload = "{\"error\": \"Failed to serialize auth data\"}";
            }

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(jsonPayload));
                        session.close(CloseStatus.NORMAL);
                    } catch (IOException e) {
                        // ignore or log
                    }
                }
            }
        }
    }
}
