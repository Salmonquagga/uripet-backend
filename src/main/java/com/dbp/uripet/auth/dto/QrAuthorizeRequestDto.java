package com.dbp.uripet.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrAuthorizeRequestDto {
    @NotBlank(message = "QR authorization token is required")
    private String token;
}
