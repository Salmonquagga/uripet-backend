package com.dbp.uripet.auth.controller;

import com.dbp.uripet.auth.dto.QrAuthorizeRequestDto;
import com.dbp.uripet.auth.dto.QrSessionResponseDto;
import com.dbp.uripet.auth.service.QrLoginService;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/qr")
@RequiredArgsConstructor
public class QrLoginController {

    private final QrLoginService qrLoginService;
    private final UserService userService;

    @PostMapping("/session")
    @Operation(summary = "Create a new QR Code login session")
    public ResponseEntity<QrSessionResponseDto> createSession() {
        return ResponseEntity.ok(qrLoginService.createSession());
    }

    @PostMapping("/authorize")
    @Operation(summary = "Authorize a QR Code session using an authenticated mobile/user client")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<QrSessionResponseDto> authorize(@Valid @RequestBody QrAuthorizeRequestDto request) {
        User user = userService.getAuthenticatedUser();
        return ResponseEntity.ok(qrLoginService.authorizeSession(request.getToken(), user));
    }

    @GetMapping("/status/{token}")
    @Operation(summary = "Check the status of a QR Code session")
    public ResponseEntity<QrSessionResponseDto> getStatus(@PathVariable String token) {
        return ResponseEntity.ok(qrLoginService.getSessionStatus(token));
    }
}
