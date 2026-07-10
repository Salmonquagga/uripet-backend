package com.dbp.uripet.user.controller;

import com.dbp.uripet.auth.dto.RegisterRequestDto;
import com.dbp.uripet.user.dto.UserPreferencesRequestDto;
import com.dbp.uripet.user.dto.UserPreferencesResponseDto;
import com.dbp.uripet.user.dto.UserResponseDto;
import com.dbp.uripet.user.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping("/{uid}")
    public ResponseEntity<UserResponseDto> getUser(
            @PathVariable String uid
    ) {
        return ResponseEntity.ok(
                userService.getUser(uid)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMe() {
        return ResponseEntity.ok(
                userService.getMe()
        );
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponseDto> updateMe(
            @Valid @RequestBody RegisterRequestDto request
    ) {
        return ResponseEntity.ok(
                userService.updateMe(request)
        );
    }

    @GetMapping("/me/preferences")
    public ResponseEntity<UserPreferencesResponseDto> getMyPreferences() {
        return ResponseEntity.ok(
                userService.getMyPreferences()
        );
    }

    @PatchMapping("/me/preferences")
    public ResponseEntity<UserPreferencesResponseDto> updateMyPreferences(
            @Valid @RequestBody UserPreferencesRequestDto request
    ) {
        return ResponseEntity.ok(
                userService.updateMyPreferences(request)
        );
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe() {
        userService.deleteMe();
        return ResponseEntity.noContent().build();
    }
}