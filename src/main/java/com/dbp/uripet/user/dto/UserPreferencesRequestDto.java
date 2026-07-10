package com.dbp.uripet.user.dto;

import com.dbp.uripet.user.domain.enums.LanguagePreference;
import com.dbp.uripet.user.domain.enums.ThemePreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesRequestDto {

    private LanguagePreference language;

    private ThemePreference theme;
}