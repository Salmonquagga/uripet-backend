package com.dbp.uripet.auth.dto;

import lombok.*;

import java.time.ZonedDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrSessionResponseDto {
    private String token;
    private String qrCodeBase64;
    private String status; // PENDING, AUTHORIZED, EXPIRED
    private ZonedDateTime expiresAt;
    private AuthResponseDto authData; // present only when AUTHORIZED
}
