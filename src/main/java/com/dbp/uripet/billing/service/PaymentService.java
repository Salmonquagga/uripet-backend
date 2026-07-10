package com.dbp.uripet.billing.service;

import com.dbp.uripet.billing.dto.PaymentWebhookRequestDto;
import com.dbp.uripet.config.error.UnauthorizedException;
import com.dbp.uripet.config.error.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

    @Service
    @RequiredArgsConstructor
    @Slf4j
    public class PaymentService {

        private final BillingService billingService;

        @Value("${payment.webhook.secret:}")
        private String paymentWebhookSecret;

        public void processWebhook(PaymentWebhookRequestDto request, String receivedSecret) {
            validateWebhookSecret(receivedSecret);
            validateRequest(request);

            String normalizedStatus = request.getStatus().trim().toUpperCase();

            switch (normalizedStatus) {
                case "PAID", "APPROVED", "SUCCESS", "CONFIRMED" -> {
                    billingService.confirmTransaction(request.getTransactionUid(), request.getProviderPaymentId());
                    log.info("Payment webhook confirmed transaction {}", request.getTransactionUid());
                }
                case "FAILED", "REJECTED", "CANCELLED" -> {
                    billingService.failTransaction(request.getTransactionUid(), request.getFailureReason());
                    log.warn("Payment webhook failed transaction {}", request.getTransactionUid());
                }
                case "PENDING" -> log.info("Payment webhook pending transaction {}", request.getTransactionUid());
                default -> throw new ValidationException("Unsupported payment status: " + request.getStatus());
            }
        }

        private void validateWebhookSecret(String receivedSecret) {
            if (!StringUtils.hasText(paymentWebhookSecret)) {
                return;
            }

            if (!StringUtils.hasText(receivedSecret) || !paymentWebhookSecret.equals(receivedSecret)) {
                throw new UnauthorizedException("Invalid payment webhook secret");
            }
        }

        private void validateRequest(PaymentWebhookRequestDto request) {
            if (request == null) {
                throw new ValidationException("Webhook request is required");
            }

            if (!StringUtils.hasText(request.getTransactionUid())) {
                throw new ValidationException("Transaction UID is required");
            }

            if (!StringUtils.hasText(request.getStatus())) {
                throw new ValidationException("Payment status is required");
            }
        }
    }