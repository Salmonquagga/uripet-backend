package com.dbp.uripet.auth.service;

import com.dbp.uripet.auth.domain.QrLoginSession;
import com.dbp.uripet.auth.dto.AuthResponseDto;
import com.dbp.uripet.auth.dto.QrSessionResponseDto;
import com.dbp.uripet.auth.repository.QrLoginSessionRepository;
import com.dbp.uripet.auth.websocket.QrLoginWebSocketHandler;
import com.dbp.uripet.config.error.ConflictException;
import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.config.jwt.JwtService;
import com.dbp.uripet.user.domain.User;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrLoginService {

    private final QrLoginSessionRepository qrLoginSessionRepository;
    private final QrLoginWebSocketHandler qrLoginWebSocketHandler;
    private final JwtService jwtService;

    @Transactional
    public QrSessionResponseDto createSession() {
        String token = UUID.randomUUID().toString();
        ZonedDateTime expiresAt = ZonedDateTime.now().plusMinutes(5);

        QrLoginSession session = QrLoginSession.builder()
                .token(token)
                .status("PENDING")
                .expiresAt(expiresAt)
                .build();

        qrLoginSessionRepository.save(session);

        String qrCodeBase64 = generateQrCodeBase64(token);

        return QrSessionResponseDto.builder()
                .token(token)
                .qrCodeBase64(qrCodeBase64)
                .status("PENDING")
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional
    public QrSessionResponseDto authorizeSession(String token, User user) {
        QrLoginSession session = qrLoginSessionRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("QR Session not found"));

        if ("EXPIRED".equals(session.getStatus()) || session.getExpiresAt().isBefore(ZonedDateTime.now())) {
            session.setStatus("EXPIRED");
            qrLoginSessionRepository.save(session);
            throw new ConflictException("QR code has expired");
        }

        if ("AUTHORIZED".equals(session.getStatus())) {
            throw new ConflictException("QR code is already authorized");
        }

        String jwtToken = jwtService.generateToken(user);

        session.setStatus("AUTHORIZED");
        session.setUser(user);
        session.setJwtToken(jwtToken);
        qrLoginSessionRepository.save(session);

        AuthResponseDto authResponse = AuthResponseDto.builder()
                .token(jwtToken)
                .uid(user.getUid())
                .verified(user.isVerified())
                .build();

        QrSessionResponseDto response = QrSessionResponseDto.builder()
                .token(token)
                .status("AUTHORIZED")
                .expiresAt(session.getExpiresAt())
                .authData(authResponse)
                .build();

        // Notify client listening via WebSocket
        qrLoginWebSocketHandler.notifyClient(token, response);

        return response;
    }

    @Transactional
    public QrSessionResponseDto getSessionStatus(String token) {
        QrLoginSession session = qrLoginSessionRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("QR Session not found"));

        if ("PENDING".equals(session.getStatus()) && session.getExpiresAt().isBefore(ZonedDateTime.now())) {
            session.setStatus("EXPIRED");
            qrLoginSessionRepository.save(session);
        }

        AuthResponseDto authResponse = null;
        if ("AUTHORIZED".equals(session.getStatus())) {
            authResponse = AuthResponseDto.builder()
                    .token(session.getJwtToken())
                    .uid(session.getUser().getUid())
                    .verified(session.getUser().isVerified())
                    .build();
        }

        return QrSessionResponseDto.builder()
                .token(session.getToken())
                .status(session.getStatus())
                .expiresAt(session.getExpiresAt())
                .authData(authResponse)
                .build();
    }

    private String generateQrCodeBase64(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
        } catch (Exception e) {
            throw new com.dbp.uripet.config.error.ServerErrorException("Failed to generate QR code image", e);
        }
    }
}
