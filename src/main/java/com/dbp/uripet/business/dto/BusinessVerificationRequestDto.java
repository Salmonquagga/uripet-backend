package com.dbp.uripet.business.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BusinessVerificationRequestDto {

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotBlank(message = "Business type is required")
    private String businessType;

    private String ruc;

    @Email(message = "Contact email must be valid")
    private String contactEmail;

    private String phone;
    private String address;
    private String documentUrl;
    private String description;
}
