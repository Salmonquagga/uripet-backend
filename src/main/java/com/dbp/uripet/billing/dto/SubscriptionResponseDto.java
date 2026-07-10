package com.dbp.uripet.billing.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Builder
public class SubscriptionResponseDto {
    private String uid;
    private String workspaceUid;
    private String workspaceName;
    private String planType;
    private String status;
    private BigDecimal amount;
    private String currency;
    private ZonedDateTime startedAt;
    private ZonedDateTime nextBillingAt;
    private ZonedDateTime cancelledAt;
    private ZonedDateTime createdAt;
}