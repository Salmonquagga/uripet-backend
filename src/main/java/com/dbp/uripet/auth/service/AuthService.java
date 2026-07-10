package com.dbp.uripet.auth.service;

import com.dbp.uripet.auth.dto.AuthRequestDto;
import com.dbp.uripet.auth.dto.AuthResponseDto;
import com.dbp.uripet.auth.dto.RegisterRequestDto;
import com.dbp.uripet.auth.dto.VerificationRequestDto;
import com.dbp.uripet.auth.dto.VerificationResponseDto;
import com.dbp.uripet.config.error.DuplicateResourceException;
import com.dbp.uripet.config.error.InvalidOperationException;
import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.config.jwt.JwtService;
import com.dbp.uripet.events.UserRegisteredEvent;
import com.dbp.uripet.events.UserVerifiedEvent;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.user.domain.enums.UserRole;
import com.dbp.uripet.user.repository.UserRepository;
import com.dbp.uripet.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final com.dbp.uripet.user.service.UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ApplicationEventPublisher eventPublisher;
    private final WorkspaceService workspaceService;

    public AuthResponseDto login(AuthRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String token = jwtService.generateToken(user);
        return AuthResponseDto.builder()
                .token(token)
                .uid(user.getUid())
                .verified(user.isVerified())
                .build();
    }

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }
        if (userRepository.findByName(request.getName()).isPresent()) {
            throw new DuplicateResourceException("Name already exists");
        }

        String verificationCode = generateVerificationCode();
        ZonedDateTime verificationCodeExpiresAt = ZonedDateTime.now().plusMinutes(10);

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .verified(false)
                .role(UserRole.USER)
                .verificationCode(verificationCode)
                .verificationCodeExpiresAt(verificationCodeExpiresAt)
                .build();

        User savedUser = userRepository.save(user);

        // Crea automáticamente el espacio personal gratis del usuario.
        workspaceService.createPersonalWorkspaceForUser(savedUser);

        eventPublisher.publishEvent(
                new UserRegisteredEvent(savedUser.getEmail(), savedUser.getName(), verificationCode)
        );

        String token = jwtService.generateToken(savedUser);
        return AuthResponseDto.builder()
                .token(token)
                .uid(savedUser.getUid())
                .verified(savedUser.isVerified())
                .build();
    }

    public VerificationResponseDto verifyAccount(VerificationRequestDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.isVerified()) {
            throw new DuplicateResourceException("Account already verified");
        }

        if (user.getVerificationCode() == null
                || user.getVerificationCodeExpiresAt() == null
                || !user.getVerificationCode().equals(request.getCode())
                || user.getVerificationCodeExpiresAt().isBefore(ZonedDateTime.now())) {
            throw new InvalidOperationException("Invalid or expired verification code");
        }

        user.setVerified(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        eventPublisher.publishEvent(new UserVerifiedEvent(user.getEmail(), user.getName()));

        return VerificationResponseDto.builder()
                .uid(user.getUid())
                .verified(true)
                .message("Account verified successfully")
                .build();
    }

    public VerificationResponseDto resendVerificationCode() {
        User user = userService.getAuthenticatedUserAllowingUnverified();

        if (user.isVerified()) {
            throw new InvalidOperationException("Account already verified");
        }

        String verificationCode = generateVerificationCode();
        user.setVerificationCode(verificationCode);
        user.setVerificationCodeExpiresAt(ZonedDateTime.now().plusMinutes(10));
        userRepository.save(user);

        eventPublisher.publishEvent(new UserRegisteredEvent(user.getEmail(), user.getName(), verificationCode));

        return VerificationResponseDto.builder()
                .uid(user.getUid())
                .verified(false)
                .message("Verification code resent successfully")
                .build();
    }

    private String generateVerificationCode() {
        return String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
    }
}
