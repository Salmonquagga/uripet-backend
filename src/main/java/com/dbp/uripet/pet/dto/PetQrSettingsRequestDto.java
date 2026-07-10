package com.dbp.uripet.pet.dto;

import com.dbp.uripet.pet.domain.enums.QrCornerStyle;
import com.dbp.uripet.pet.domain.enums.QrFrameStyle;
import com.dbp.uripet.pet.domain.enums.QrPatternStyle;
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
public class PetQrSettingsRequestDto {

    /**
     * Color principal del QR.
     *
     * Formato hexadecimal:
     * #000000
     * #0A6148
     */
    @Pattern(
            regexp = "^#[0-9A-Fa-f]{6}$",
            message = "Foreground color must use hexadecimal format #RRGGBB"
    )
    private String foregroundColor;

    /**
     * Color de fondo del QR.
     */
    @Pattern(
            regexp = "^#[0-9A-Fa-f]{6}$",
            message = "Background color must use hexadecimal format #RRGGBB"
    )
    private String backgroundColor;

    /**
     * Estilo de los puntos o módulos del QR.
     */
    private QrPatternStyle patternStyle;

    /**
     * Estilo de los marcadores de las esquinas.
     */
    private QrCornerStyle cornerStyle;

    /**
     * Estilo del marco exterior.
     */
    private QrFrameStyle frameStyle;

    /**
     * Permite mostrar la imagen principal
     * de la mascota en el centro del QR.
     */
    private Boolean showPetImage;

    /**
     * Permite mostrar un texto debajo del QR.
     */
    private Boolean showLabel;

    /**
     * Texto mostrado debajo del QR.
     */
    @Size(
            max = 40,
            message = "QR label cannot exceed 40 characters"
    )
    private String labelText;
}