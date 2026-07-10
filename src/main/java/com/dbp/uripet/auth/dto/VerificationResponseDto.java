package com.dbp.uripet.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationResponseDto {
    private String uid;
    private boolean verified;
    private String message;
}