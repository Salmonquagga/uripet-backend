package com.dbp.uripet.billing.controller;

import com.dbp.uripet.billing.dto.CheckoutResponseDto;
import com.dbp.uripet.billing.dto.CreateCheckoutRequestDto;
import com.dbp.uripet.billing.dto.PaymentTransactionResponseDto;
import com.dbp.uripet.billing.dto.ReactivateSubscriptionRequestDto;
import com.dbp.uripet.billing.dto.SubscriptionResponseDto;
import com.dbp.uripet.billing.service.BillingService;
import com.dbp.uripet.user.domain.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BillingController {

    private final BillingService billingService;

    /**
     * Crea un workspace nuevo FAMILY o PREMIUM,
     * una suscripción pendiente y una transacción.
     */
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponseDto>
    createCheckout(
            @Valid
            @RequestBody
            CreateCheckoutRequestDto request,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return new ResponseEntity<>(
                billingService.createCheckout(
                        request,
                        currentUser
                ),
                HttpStatus.CREATED
        );
    }

    /**
     * Reactiva un workspace congelado o vencido.
     */
    @PostMapping(
            "/subscriptions/workspace/{workspaceUid}/reactivate"
    )
    public ResponseEntity<CheckoutResponseDto>
    reactivateSubscription(
            @PathVariable
            String workspaceUid,

            @Valid
            @RequestBody(required = false)
            ReactivateSubscriptionRequestDto request,

            @AuthenticationPrincipal
            User currentUser
    ) {
        ReactivateSubscriptionRequestDto safeRequest =
                request != null
                        ? request
                        : ReactivateSubscriptionRequestDto
                          .builder()
                          .paymentMethodType("MOCK")
                          .build();

        return new ResponseEntity<>(
                billingService.reactivateSubscription(
                        workspaceUid,
                        safeRequest,
                        currentUser
                ),
                HttpStatus.CREATED
        );
    }

    /**
     * Solo el OWNER puede consultar la suscripción.
     */
    @GetMapping(
            "/subscriptions/workspace/{workspaceUid}"
    )
    public ResponseEntity<SubscriptionResponseDto>
    getCurrentSubscription(
            @PathVariable
            String workspaceUid,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                billingService
                        .getCurrentSubscriptionByWorkspace(
                                workspaceUid,
                                currentUser
                        )
        );
    }

    /**
     * Solo el OWNER puede consultar el historial
     * de transacciones del grupo.
     */
    @GetMapping(
            "/transactions/workspace/{workspaceUid}"
    )
    public ResponseEntity<
            List<PaymentTransactionResponseDto>
            >
    getTransactions(
            @PathVariable
            String workspaceUid,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                billingService
                        .getTransactionsByWorkspace(
                                workspaceUid,
                                currentUser
                        )
        );
    }

    /**
     * Endpoint exclusivo para pruebas.
     *
     * En producción deberá desactivarse o protegerse
     * mediante un perfil de Spring.
     */
    @PostMapping(
            "/mock-confirm/{transactionUid}"
    )
    public ResponseEntity<PaymentTransactionResponseDto>
    mockConfirmPayment(
            @PathVariable
            String transactionUid,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                billingService.confirmMockTransaction(
                        transactionUid,
                        currentUser
                )
        );
    }

    /**
     * Endpoint exclusivo para pruebas.
     */
    @PostMapping(
            "/mock-fail/{transactionUid}"
    )
    public ResponseEntity<PaymentTransactionResponseDto>
    mockFailPayment(
            @PathVariable
            String transactionUid,

            @RequestBody(required = false)
            Map<String, String> body,

            @AuthenticationPrincipal
            User currentUser
    ) {
        String reason =
                body != null
                        ? body.get("reason")
                        : null;

        return ResponseEntity.ok(
                billingService.failMockTransaction(
                        transactionUid,
                        reason,
                        currentUser
                )
        );
    }

    /**
     * Cancela la suscripción y congela el workspace.
     *
     * No elimina mascotas, miembros ni historial.
     */
    @PostMapping(
            "/subscriptions/workspace/{workspaceUid}/cancel"
    )
    public ResponseEntity<SubscriptionResponseDto>
    cancelSubscription(
            @PathVariable
            String workspaceUid,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                billingService.cancelSubscription(
                        workspaceUid,
                        currentUser
                )
        );
    }
}