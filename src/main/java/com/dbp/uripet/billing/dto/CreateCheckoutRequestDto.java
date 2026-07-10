package com.dbp.uripet.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCheckoutRequestDto {

    /**
     * Nombre del nuevo grupo que se creará.
     *
     * Este endpoint se utiliza solamente para
     * crear un nuevo workspace FAMILY o PREMIUM.
     */
    @NotBlank(
            message = "Workspace name is required"
    )
    @Size(
            min = 2,
            max = 80,
            message = "Workspace name must contain between 2 and 80 characters"
    )
    private String workspaceName;

    /**
     * Solo se aceptan FAMILY y PREMIUM.
     *
     * FREE se crea automáticamente al registrarse.
     * VETERINARY y SHELTER requieren verificación.
     */
    @NotBlank(
            message = "Plan type is required"
    )
    @Pattern(
            regexp = "^(FAMILY|PREMIUM)$",
            message = "Plan type must be FAMILY or PREMIUM"
    )
    private String planType;

    /**
     * En la versión actual se utiliza MOCK.
     *
     * El campo queda preparado para una pasarela real.
     */
    @Pattern(
            regexp = "^(MOCK|CARD|YAPE|PLIN|TRANSFER)$",
            message = "Payment method type must be MOCK, CARD, YAPE, PLIN or TRANSFER"
    )
    @Builder.Default
    private String paymentMethodType = "MOCK";
}