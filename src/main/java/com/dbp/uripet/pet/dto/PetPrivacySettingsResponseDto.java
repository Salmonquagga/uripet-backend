package com.dbp.uripet.pet.dto;

import com.dbp.uripet.workspace.domain.enums.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetPrivacySettingsResponseDto {

    private String petPid;

    private boolean showEmergencyContact;

    private boolean showBasicInformation;

    private boolean showHealthSummary;

    private boolean healthSummaryAvailableForPlan;

    private PlanType planType;
}