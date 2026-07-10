package com.dbp.uripet.pet.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicPetResponseDto {

    /*
     * Siempre visibles para que el perfil público
     * pueda identificar a la mascota.
     */
    private String pid;

    private String name;

    private String mainImageUrl;

    /*
     * Solamente se devuelven cuando
     * showBasicInformation = true.
     */
    private String species;

    private String breed;

    private LocalDate birthDate;

    private String color;

    /*
     * Solamente se devuelve cuando
     * showEmergencyContact = true.
     */
    private String emergencyContact;

    /*
     * Solamente se devuelve cuando:
     *
     * 1. showHealthSummary = true
     * 2. El plan permite PUBLIC_HEALTH_SUMMARY
     * 3. El workspace está activo
     */
    private List<PublicPetHealthSummaryDto> healthSummary;
}