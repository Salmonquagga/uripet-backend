package com.dbp.uripet.workspace.service;

import com.dbp.uripet.config.error.InvalidOperationException;
import com.dbp.uripet.pet.repository.PetRepository;
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.enums.PlanType;
import com.dbp.uripet.workspace.dto.PlanLimitsResponseDto;
import com.dbp.uripet.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanLimitsService {

    private final PetRepository petRepository;

    private final WorkspaceMemberRepository
            workspaceMemberRepository;

    public int getMaxPets(PlanType planType) {
        validatePlanType(planType);

        return switch (planType) {
            case FREE -> 1;
            case FAMILY -> 5;
            case PREMIUM -> 10;
            case VETERINARY -> 100;
            case SHELTER -> 250;
        };
    }

    public int getMaxMembers(PlanType planType) {
        validatePlanType(planType);

        return switch (planType) {
            case FREE -> 1;
            case FAMILY -> 5;
            case PREMIUM -> 10;
            case VETERINARY -> 20;
            case SHELTER -> 30;
        };
    }

    public void checkPetLimit(Workspace workspace) {
        validateWorkspace(workspace);

        long currentPets =
                petRepository.countByWorkspace(workspace);

        int maxPets =
                getMaxPets(workspace.getPlanType());

        if (currentPets >= maxPets) {
            throw new InvalidOperationException(
                    "Pet limit reached for "
                            + workspace.getPlanType().name()
                            + " plan. Maximum allowed: "
                            + maxPets
            );
        }
    }

    public void checkMemberLimit(Workspace workspace) {
        validateWorkspace(workspace);

        long currentMembers =
                workspaceMemberRepository
                        .countByWorkspaceAndActiveTrue(
                                workspace
                        );

        int maxMembers =
                getMaxMembers(
                        workspace.getPlanType()
                );

        if (currentMembers >= maxMembers) {
            throw new InvalidOperationException(
                    "Member limit reached for "
                            + workspace.getPlanType().name()
                            + " plan. Maximum allowed: "
                            + maxMembers
            );
        }
    }

    public PlanLimitsResponseDto getLimits(
            Workspace workspace
    ) {
        validateWorkspace(workspace);

        int maxPets =
                getMaxPets(workspace.getPlanType());

        int maxMembers =
                getMaxMembers(
                        workspace.getPlanType()
                );

        long currentPets =
                petRepository.countByWorkspace(workspace);

        long currentMembers =
                workspaceMemberRepository
                        .countByWorkspaceAndActiveTrue(
                                workspace
                        );

        int remainingPets =
                calculateRemaining(
                        maxPets,
                        currentPets
                );

        int remainingMembers =
                calculateRemaining(
                        maxMembers,
                        currentMembers
                );

        return PlanLimitsResponseDto.builder()
                .planType(
                        workspace.getPlanType()
                )
                .maxPets(maxPets)
                .maxMembers(maxMembers)
                .currentPets(currentPets)
                .currentMembers(currentMembers)
                .remainingPets(remainingPets)
                .remainingMembers(
                        remainingMembers
                )
                .petLimitReached(
                        currentPets >= maxPets
                )
                .memberLimitReached(
                        currentMembers >= maxMembers
                )
                .build();
    }

    public PlanLimitsResponseDto getEmptyLimits(
            PlanType planType
    ) {
        int maxPets = getMaxPets(planType);
        int maxMembers = getMaxMembers(planType);

        return PlanLimitsResponseDto.builder()
                .planType(planType)
                .maxPets(maxPets)
                .maxMembers(maxMembers)
                .currentPets(0)
                .currentMembers(0)
                .remainingPets(maxPets)
                .remainingMembers(maxMembers)
                .petLimitReached(false)
                .memberLimitReached(false)
                .build();
    }

    private int calculateRemaining(
            int maximum,
            long current
    ) {
        long remaining = maximum - current;

        if (remaining <= 0) {
            return 0;
        }

        if (remaining > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) remaining;
    }

    private void validateWorkspace(
            Workspace workspace
    ) {
        if (workspace == null) {
            throw new InvalidOperationException(
                    "Workspace is required"
            );
        }

        validatePlanType(
                workspace.getPlanType()
        );
    }

    private void validatePlanType(
            PlanType planType
    ) {
        if (planType == null) {
            throw new InvalidOperationException(
                    "Plan type is required"
            );
        }
    }
}