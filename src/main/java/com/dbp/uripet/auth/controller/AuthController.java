package com.dbp.uripet.auth.controller;
import com.dbp.uripet.auth.dto.AuthRequestDto;
import com.dbp.uripet.auth.dto.AuthResponseDto;
import com.dbp.uripet.auth.dto.RegisterRequestDto;
import com.dbp.uripet.auth.dto.VerificationRequestDto;
import com.dbp.uripet.auth.dto.VerificationResponseDto;
import com.dbp.uripet.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/verify")
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<VerificationResponseDto> verify(@Valid @RequestBody VerificationRequestDto request) {
        return ResponseEntity.ok(authService.verifyAccount(request));
    }

    @PostMapping("/resend-verification")
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<VerificationResponseDto> resendVerification() {
        return ResponseEntity.ok(authService.resendVerificationCode());
    }
}
