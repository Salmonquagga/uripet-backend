package com.dbp.uripet.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerificationRequestDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid format")
    private String email;

    @NotBlank(message = "Verification code is required")
    @Size(min = 4, max = 10, message = "Verification code must be between 4 and 10 characters")
    private String code;
}