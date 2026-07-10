package com.dbp.uripet.billing.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
@Builder
public class PaymentTransactionResponseDto {
    private String uid;
    private String workspaceUid;
    private String subscriptionUid;
    private String provider;
    private String status;
    private String providerPaymentId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String failureReason;
    private ZonedDateTime createdAt;
    private ZonedDateTime paidAt;
}