package com.dbp.uripet.auth.service;

import com.dbp.uripet.auth.domain.QrLoginSession;
import com.dbp.uripet.auth.dto.QrSessionResponseDto;
import com.dbp.uripet.auth.repository.QrLoginSessionRepository;
import com.dbp.uripet.auth.websocket.QrLoginWebSocketHandler;
import com.dbp.uripet.config.error.ConflictException;
import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.config.jwt.JwtService;
import com.dbp.uripet.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrLoginServiceTest {

    @Mock
    private QrLoginSessionRepository qrLoginSessionRepository;

    @Mock
    private QrLoginWebSocketHandler qrLoginWebSocketHandler;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private QrLoginService qrLoginService;

    @Test
    void createSession_shouldSaveAndReturnResponseWithQrBase64() {
        QrSessionResponseDto response = qrLoginService.createSession();

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("PENDING", response.getStatus());
        assertNotNull(response.getExpiresAt());
        assertTrue(response.getQrCodeBase64().startsWith("data:image/png;base64,"));

        verify(qrLoginSessionRepository, times(1)).save(any(QrLoginSession.class));
    }

    @Test
    void authorizeSession_whenSessionIsValid_shouldAuthorizeAndNotify() {
        String token = "test-token";
        User user = new User();
        user.setUid("USR-123");
        user.setEmail("test@example.com");
        user.setVerified(true);

        QrLoginSession session = QrLoginSession.builder()
                .token(token)
                .status("PENDING")
                .expiresAt(ZonedDateTime.now().plusMinutes(5))
                .build();

        when(qrLoginSessionRepository.findByToken(token)).thenReturn(Optional.of(session));
        when(jwtService.generateToken(user)).thenReturn("mocked-jwt-token");

        QrSessionResponseDto response = qrLoginService.authorizeSession(token, user);

        assertNotNull(response);
        assertEquals("AUTHORIZED", response.getStatus());
        assertNotNull(response.getAuthData());
        assertEquals("mocked-jwt-token", response.getAuthData().getToken());
        assertEquals("USR-123", response.getAuthData().getUid());
        assertTrue(response.getAuthData().isVerified());

        verify(qrLoginSessionRepository, times(1)).save(session);
        verify(qrLoginWebSocketHandler, times(1)).notifyClient(eq(token), any(QrSessionResponseDto.class));
    }

    @Test
    void authorizeSession_whenSessionExpired_shouldMarkExpiredAndThrowConflict() {
        String token = "expired-token";
        User user = new User();

        QrLoginSession session = QrLoginSession.builder()
                .token(token)
                .status("PENDING")
                .expiresAt(ZonedDateTime.now().minusSeconds(10))
                .build();

        when(qrLoginSessionRepository.findByToken(token)).thenReturn(Optional.of(session));

        assertThrows(ConflictException.class, () -> qrLoginService.authorizeSession(token, user));

        assertEquals("EXPIRED", session.getStatus());
        verify(qrLoginSessionRepository, times(1)).save(session);
        verify(qrLoginWebSocketHandler, never()).notifyClient(anyString(), any());
    }

    @Test
    void authorizeSession_whenAlreadyAuthorized_shouldThrowConflict() {
        String token = "auth-token";
        User user = new User();

        QrLoginSession session = QrLoginSession.builder()
                .token(token)
                .status("AUTHORIZED")
                .expiresAt(ZonedDateTime.now().plusMinutes(5))
                .build();

        when(qrLoginSessionRepository.findByToken(token)).thenReturn(Optional.of(session));

        assertThrows(ConflictException.class, () -> qrLoginService.authorizeSession(token, user));

        verify(qrLoginSessionRepository, never()).save(any());
        verify(qrLoginWebSocketHandler, never()).notifyClient(anyString(), any());
    }

    @Test
    void getSessionStatus_whenSessionIsPendingAndNotExpired_shouldReturnPending() {
        String token = "pending-token";
        QrLoginSession session = QrLoginSession.builder()
                .token(token)
                .status("PENDING")
                .expiresAt(ZonedDateTime.now().plusMinutes(5))
                .build();

        when(qrLoginSessionRepository.findByToken(token)).thenReturn(Optional.of(session));

        QrSessionResponseDto response = qrLoginService.getSessionStatus(token);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertNull(response.getAuthData());
        verify(qrLoginSessionRepository, never()).save(any());
    }

    @Test
    void getSessionStatus_whenSessionIsPendingAndExpired_shouldMarkExpiredAndReturnExpired() {
        String token = "expired-token";
        QrLoginSession session = QrLoginSession.builder()
                .token(token)
                .status("PENDING")
                .expiresAt(ZonedDateTime.now().minusSeconds(10))
                .build();

        when(qrLoginSessionRepository.findByToken(token)).thenReturn(Optional.of(session));

        QrSessionResponseDto response = qrLoginService.getSessionStatus(token);

        assertNotNull(response);
        assertEquals("EXPIRED", response.getStatus());
        assertNull(response.getAuthData());
        verify(qrLoginSessionRepository, times(1)).save(session);
    }
}
