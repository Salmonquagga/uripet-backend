package com.dbp.uripet.billing.controller;

import com.dbp.uripet.billing.dto.PaymentWebhookRequestDto;
import com.dbp.uripet.billing.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/billing/webhook")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Map<String, Object>>
    receiveWebhook(
            @Valid
            @RequestBody
            PaymentWebhookRequestDto request,

            @RequestHeader(
                    value = "X-Webhook-Secret",
                    required = false
            )
            String webhookSecret
    ) {
        paymentService.processWebhook(
                request,
                webhookSecret
        );

        return ResponseEntity.ok(
                Map.of(
                        "received",
                        true,
                        "message",
                        "Payment webhook processed successfully"
                )
        );
    }
}