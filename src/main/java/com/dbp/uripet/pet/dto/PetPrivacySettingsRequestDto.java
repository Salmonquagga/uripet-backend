package com.dbp.uripet.pet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetPrivacySettingsRequestDto {

    private Boolean showEmergencyContact;

    private Boolean showBasicInformation;

    private Boolean showHealthSummary;
}