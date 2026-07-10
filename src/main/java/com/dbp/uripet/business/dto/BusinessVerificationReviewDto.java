package com.dbp.uripet.business.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BusinessVerificationReviewDto {

    @NotBlank(message = "Status is required")
    private String status;

    private String reviewComment;
}
