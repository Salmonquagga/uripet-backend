package com.dbp.uripet.billing.service;

import com.dbp.uripet.billing.dto.PaymentWebhookRequestDto;
import com.dbp.uripet.config.error.ServerErrorException;
import com.dbp.uripet.config.error.UnauthorizedException;
import com.dbp.uripet.config.error.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final BillingService billingService;

    @Value("${payment.webhook.secret:}")
    private String paymentWebhookSecret;

    public void processWebhook(
            PaymentWebhookRequestDto request,
            String receivedSecret
    ) {
        validateWebhookSecret(
                receivedSecret
        );

        validateRequest(request);

        String normalizedStatus =
                request
                        .getStatus()
                        .trim()
                        .toUpperCase();

        switch (normalizedStatus) {

            case "PAID",
                 "APPROVED",
                 "SUCCESS",
                 "CONFIRMED" -> {

                billingService.confirmTransaction(
                        request.getTransactionUid(),
                        clean(
                                request
                                        .getProviderPaymentId()
                        )
                );

                log.info(
                        "Payment webhook confirmed transaction {}",
                        request.getTransactionUid()
                );
            }

            case "FAILED",
                 "REJECTED",
                 "CANCELLED" -> {

                billingService.failTransaction(
                        request.getTransactionUid(),
                        clean(
                                request
                                        .getFailureReason()
                        )
                );

                log.warn(
                        "Payment webhook marked transaction {} as failed",
                        request.getTransactionUid()
                );
            }

            case "PENDING" ->
                    log.info(
                            "Payment webhook left transaction {} pending",
                            request.getTransactionUid()
                    );

            default ->
                    throw new ValidationException(
                            "Unsupported payment status: "
                                    + request.getStatus()
                    );
        }
    }

    private void validateWebhookSecret(
            String receivedSecret
    ) {
        /*
         * Fallamos de forma segura.
         * Si el servidor no tiene configurado
         * el secreto, el webhook no puede operar.
         */
        if (!StringUtils.hasText(
                paymentWebhookSecret
        )) {
            log.error(
                    "PAYMENT_WEBHOOK_SECRET is not configured"
            );

            throw new ServerErrorException(
                    "Payment webhook is not configured"
            );
        }

        if (!StringUtils.hasText(
                receivedSecret
        )) {
            throw new UnauthorizedException(
                    "Payment webhook secret is required"
            );
        }

        byte[] expected =
                paymentWebhookSecret
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        byte[] received =
                receivedSecret
                        .getBytes(
                                StandardCharsets.UTF_8
                        );

        /*
         * Comparación constante para reducir
         * filtraciones por tiempo de respuesta.
         */
        boolean valid =
                MessageDigest.isEqual(
                        expected,
                        received
                );

        if (!valid) {
            throw new UnauthorizedException(
                    "Invalid payment webhook secret"
            );
        }
    }

    private void validateRequest(
            PaymentWebhookRequestDto request
    ) {
        if (request == null) {
            throw new ValidationException(
                    "Webhook request is required"
            );
        }

        if (!StringUtils.hasText(
                request.getEventType()
        )) {
            throw new ValidationException(
                    "Event type is required"
            );
        }

        if (!StringUtils.hasText(
                request.getTransactionUid()
        )) {
            throw new ValidationException(
                    "Transaction UID is required"
            );
        }

        if (!StringUtils.hasText(
                request.getStatus()
        )) {
            throw new ValidationException(
                    "Payment status is required"
            );
        }

        if (request.getAmount() != null
                && request
                .getAmount()
                .signum() < 0) {

            throw new ValidationException(
                    "Amount cannot be negative"
            );
        }
    }

    private String clean(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}