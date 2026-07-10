package com.dbp.uripet.user.service;

import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.config.error.UnauthorizedException;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAuthenticatedUser_whenAuthenticationIsNull_shouldThrowUnauthorizedException() {
        when(securityContext.getAuthentication()).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> userService.getAuthenticatedUser());
    }

    @Test
    void getAuthenticatedUser_whenNotAuthenticated_shouldThrowUnauthorizedException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> userService.getAuthenticatedUser());
    }

    @Test
    void getAuthenticatedUser_whenAnonymousAuthenticationToken_shouldThrowUnauthorizedException() {
        AnonymousAuthenticationToken anonymousToken = mock(AnonymousAuthenticationToken.class);
        when(securityContext.getAuthentication()).thenReturn(anonymousToken);

        assertThrows(UnauthorizedException.class, () -> userService.getAuthenticatedUser());
    }

    @Test
    void getAuthenticatedUser_whenNameIsAnonymousUser_shouldThrowUnauthorizedException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("anonymousUser");

        assertThrows(UnauthorizedException.class, () -> userService.getAuthenticatedUser());
    }

    @Test
    void getAuthenticatedUser_whenAuthenticatedAndUserExists_shouldReturnUser() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setVerified(true);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        User result = userService.getAuthenticatedUser();

        assertNotNull(result);
        assertEquals("user@example.com", result.getEmail());
    }

    @Test
    void getAuthenticatedUser_whenAuthenticatedAndUserDoesNotExist_shouldThrowResourceNotFoundException() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getAuthenticatedUser());
    }
}
