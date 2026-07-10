package com.dbp.uripet.user.dto;

import com.dbp.uripet.user.domain.enums.LanguagePreference;
import com.dbp.uripet.user.domain.enums.ThemePreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private String uid;

    private String name;

    private String email;

    private String phone;

    private boolean verified;

    private LanguagePreference preferredLanguage;

    private ThemePreference preferredTheme;

    private ZonedDateTime createdAt;
}