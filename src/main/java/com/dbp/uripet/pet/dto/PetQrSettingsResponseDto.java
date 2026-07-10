package com.dbp.uripet.pet.dto;

import com.dbp.uripet.pet.domain.enums.QrCornerStyle;
import com.dbp.uripet.pet.domain.enums.QrFrameStyle;
import com.dbp.uripet.pet.domain.enums.QrPatternStyle;
import com.dbp.uripet.workspace.domain.enums.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetQrSettingsResponseDto {

    private String petPid;

    /**
     * Contenido que debe codificar el QR.
     *
     * Por ahora se devuelve el PID.
     * El frontend puede construir la URL pública.
     */
    private String qrContent;

    private String foregroundColor;

    private String backgroundColor;

    private QrPatternStyle patternStyle;

    private QrCornerStyle cornerStyle;

    private QrFrameStyle frameStyle;

    private boolean showPetImage;

    private boolean showLabel;

    private String labelText;

    private String petImageUrl;

    private PlanType planType;

    /**
     * Indica si el usuario puede modificar colores.
     */
    private boolean customColorsAvailable;

    /**
     * Indica si el usuario puede modificar
     * estilos y formas.
     */
    private boolean customStylesAvailable;

    /**
     * Indica si puede colocar la foto
     * de la mascota dentro del QR.
     */
    private boolean petImageAvailable;
}