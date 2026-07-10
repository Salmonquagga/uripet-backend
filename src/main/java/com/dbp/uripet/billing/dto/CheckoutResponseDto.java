package com.dbp.uripet.billing.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CheckoutResponseDto {
    private String workspaceUid;
    private String workspaceName;
    private String subscriptionUid;
    private String transactionUid;
    private String planType;
    private String subscriptionStatus;
    private String paymentStatus;
    private BigDecimal amount;
    private String currency;
    private String paymentUrl;
    private String message;
}