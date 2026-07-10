package com.dbp.uripet.business.dto;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class BusinessVerificationResponseDto {
    private String uid;

    private String requesterUid;
    private String requesterName;
    private String requesterEmail;

    private String reviewerUid;
    private String reviewerName;
    private String reviewerEmail;

    private String workspaceUid;
    private String workspaceName;

    private String businessName;
    private String businessType;
    private String ruc;
    private String contactEmail;
    private String phone;
    private String address;
    private String documentUrl;
    private String description;

    private String status;
    private String reviewComment;

    private ZonedDateTime createdAt;
    private ZonedDateTime reviewedAt;
}
