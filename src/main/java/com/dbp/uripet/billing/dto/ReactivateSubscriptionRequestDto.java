package com.dbp.uripet.billing.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactivateSubscriptionRequestDto {

    /**
     * En esta etapa utilizaremos MOCK.
     *
     * Se mantiene el campo preparado para conectar
     * CARD, YAPE, PLIN o TRANSFER posteriormente.
     */
    @Pattern(
            regexp = "^(MOCK|CARD|YAPE|PLIN|TRANSFER)$",
            message = "Payment method type must be MOCK, CARD, YAPE, PLIN or TRANSFER"
    )
    @Builder.Default
    private String paymentMethodType = "MOCK";
}