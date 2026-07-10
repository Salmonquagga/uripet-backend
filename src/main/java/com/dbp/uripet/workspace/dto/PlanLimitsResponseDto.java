package com.dbp.uripet.workspace.dto;

import com.dbp.uripet.workspace.domain.enums.PlanType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanLimitsResponseDto {

    private PlanType planType;

    private int maxPets;

    private int maxMembers;

    private long currentPets;

    private long currentMembers;

    private int remainingPets;

    private int remainingMembers;

    private boolean petLimitReached;

    private boolean memberLimitReached;
}