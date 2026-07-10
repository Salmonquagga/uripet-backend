package com.dbp.uripet.billing.service;

import com.dbp.uripet.config.error.InvalidOperationException;
import com.dbp.uripet.workspace.domain.enums.PlanType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PlanPricingService {

    private static final String DEFAULT_CURRENCY = "PEN";

    private static final BigDecimal FAMILY_PRICE =
            new BigDecimal("19.90");

    private static final BigDecimal PREMIUM_PRICE =
            new BigDecimal("29.90");

    /**
     * El precio siempre se decide en backend.
     *
     * El frontend nunca debe enviar ni modificar
     * el monto final del plan.
     */
    public BigDecimal getMonthlyPrice(
            PlanType planType
    ) {
        if (planType == null) {
            throw new InvalidOperationException(
                    "Plan type is required"
            );
        }

        BigDecimal price = switch (planType) {
            case FAMILY -> FAMILY_PRICE;
            case PREMIUM -> PREMIUM_PRICE;

            case FREE -> throw new InvalidOperationException(
                    "FREE plan does not require payment"
            );

            case VETERINARY, SHELTER ->
                    throw new InvalidOperationException(
                            "Business plans require an approved business verification"
                    );
        };

        return price.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    public String getCurrency(
            PlanType planType
    ) {
        if (planType == null) {
            throw new InvalidOperationException(
                    "Plan type is required"
            );
        }

        return DEFAULT_CURRENCY;
    }

    public boolean isGeneralCheckoutPlan(
            PlanType planType
    ) {
        return planType == PlanType.FAMILY
                || planType == PlanType.PREMIUM;
    }
}