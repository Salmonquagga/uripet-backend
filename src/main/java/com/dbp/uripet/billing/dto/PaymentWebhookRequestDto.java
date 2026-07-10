package com.dbp.uripet.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Data
public class PaymentWebhookRequestDto {

    @NotBlank(message = "Event type is required")
    private String eventType;

    @NotBlank(message = "Transaction UID is required")
    private String transactionUid;

    private String workspaceUid;
    private String subscriptionUid;
    private String providerPaymentId;

    @NotBlank(message = "Payment status is required")
    private String status;

    @PositiveOrZero(message = "Amount must be positive or zero")
    private BigDecimal amount;

    private String currency = "PEN";
    private ZonedDateTime occurredAt;
    private String failureReason;
}