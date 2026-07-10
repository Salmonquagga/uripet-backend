package com.dbp.uripet.user.service;

import com.dbp.uripet.auth.dto.RegisterRequestDto;
import com.dbp.uripet.config.error.ForbiddenException;
import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.config.error.UnauthorizedException;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.user.domain.enums.LanguagePreference;
import com.dbp.uripet.user.domain.enums.ThemePreference;
import com.dbp.uripet.user.dto.UserPreferencesRequestDto;
import com.dbp.uripet.user.dto.UserPreferencesResponseDto;
import com.dbp.uripet.user.dto.UserResponseDto;
import com.dbp.uripet.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {
        User user = getAuthenticatedUserAllowingUnverified();

        if (!user.isVerified()) {
            throw new ForbiddenException("Account not verified");
        }

        return user;
    }

    @Transactional(readOnly = true)
    public User getAuthenticatedUserAllowingUnverified() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || "anonymousUser".equals(authentication.getName())) {

            throw new UnauthorizedException("User is not authenticated");
        }

        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUser(String uid) {
        User user = userRepository.findByUid(uid)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        return mapToDto(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getMe() {
        return mapToDto(getAuthenticatedUser());
    }

    @Transactional
    public UserResponseDto updateMe(RegisterRequestDto request) {
        User user = getAuthenticatedUser();

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }

        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            user.setPhone(phone.isBlank() ? null : phone);
        }

        User savedUser = userRepository.save(user);

        return mapToDto(savedUser);
    }

    @Transactional(readOnly = true)
    public UserPreferencesResponseDto getMyPreferences() {
        User user = getAuthenticatedUser();

        return mapPreferencesToDto(user);
    }

    @Transactional
    public UserPreferencesResponseDto updateMyPreferences(
            UserPreferencesRequestDto request
    ) {
        User user = getAuthenticatedUser();

        if (request.getLanguage() != null) {
            user.setPreferredLanguage(request.getLanguage());
        }

        if (request.getTheme() != null) {
            user.setPreferredTheme(request.getTheme());
        }

        if (user.getPreferredLanguage() == null) {
            user.setPreferredLanguage(LanguagePreference.ES);
        }

        if (user.getPreferredTheme() == null) {
            user.setPreferredTheme(ThemePreference.SYSTEM);
        }

        User savedUser = userRepository.save(user);

        return mapPreferencesToDto(savedUser);
    }

    @Transactional
    public void deleteMe() {
        User user = getAuthenticatedUser();
        userRepository.delete(user);
    }

    private UserResponseDto mapToDto(User user) {
        return UserResponseDto.builder()
                .uid(user.getUid())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .verified(user.isVerified())
                .preferredLanguage(
                        user.getPreferredLanguage() != null
                                ? user.getPreferredLanguage()
                                : LanguagePreference.ES
                )
                .preferredTheme(
                        user.getPreferredTheme() != null
                                ? user.getPreferredTheme()
                                : ThemePreference.SYSTEM
                )
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserPreferencesResponseDto mapPreferencesToDto(User user) {
        return UserPreferencesResponseDto.builder()
                .language(
                        user.getPreferredLanguage() != null
                                ? user.getPreferredLanguage()
                                : LanguagePreference.ES
                )
                .theme(
                        user.getPreferredTheme() != null
                                ? user.getPreferredTheme()
                                : ThemePreference.SYSTEM
                )
                .build();
    }
}