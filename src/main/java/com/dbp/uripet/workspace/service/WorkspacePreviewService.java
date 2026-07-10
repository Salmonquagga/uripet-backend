package com.dbp.uripet.workspace.service;

import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.config.error.ValidationException;
import com.dbp.uripet.pet.domain.Pet;
import com.dbp.uripet.pet.repository.PetRepository;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.WorkspaceMember;
import com.dbp.uripet.workspace.domain.enums.PlanType;
import com.dbp.uripet.workspace.domain.enums.WorkspaceRole;
import com.dbp.uripet.workspace.domain.enums.WorkspaceStatus;
import com.dbp.uripet.workspace.dto.WorkspacePreviewDto;
import com.dbp.uripet.workspace.dto.WorkspacePreviewPetDto;
import com.dbp.uripet.workspace.repository.WorkspaceMemberRepository;
import com.dbp.uripet.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspacePreviewService {

    private final WorkspaceRepository
            workspaceRepository;

    private final WorkspaceMemberRepository
            workspaceMemberRepository;

    private final PetRepository petRepository;

    private final PlanAccessService
            planAccessService;

    private final PlanLimitsService
            planLimitsService;

    @Transactional(readOnly = true)
    public WorkspacePreviewDto getPreview(
            String workspaceUid,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspace(workspaceUid);

        WorkspaceMember membership =
                planAccessService
                        .checkCanAccessWorkspace(
                                workspace,
                                currentUser
                        );

        boolean owner =
                membership.getRole()
                        == WorkspaceRole.OWNER;

        boolean active =
                workspace.getStatus()
                        == WorkspaceStatus.ACTIVE;

        boolean free =
                workspace.getPlanType()
                        == PlanType.FREE;

        boolean canReactivate =
                owner
                        && !free
                        && (
                        workspace.getStatus()
                                == WorkspaceStatus.FROZEN
                                || workspace.getStatus()
                                == WorkspaceStatus.PAST_DUE
                                || workspace.getStatus()
                                == WorkspaceStatus.CANCELLED
                                || workspace.getStatus()
                                == WorkspaceStatus.PENDING_PAYMENT
                );

        List<WorkspacePreviewPetDto> pets =
                petRepository
                        .findByWorkspaceOrderByCreatedAtAsc(
                                workspace
                        )
                        .stream()
                        .map(this::toPetPreview)
                        .toList();

        return WorkspacePreviewDto.builder()
                .workspaceUid(
                        workspace.getUid()
                )
                .workspaceName(
                        workspace.getName()
                )
                .planType(
                        workspace.getPlanType()
                )
                .status(
                        workspace.getStatus()
                )
                .currentUserRole(
                        membership.getRole()
                )
                .owner(owner)
                .active(active)
                .restricted(!active)
                .canReactivate(
                        canReactivate
                )
                .restrictionMessage(
                        buildRestrictionMessage(
                                workspace.getStatus()
                        )
                )
                .petsCount(
                        pets.size()
                )
                .pets(pets)
                .limits(
                        planLimitsService.getLimits(
                                workspace
                        )
                )
                .build();
    }

    private Workspace getWorkspace(
            String workspaceUid
    ) {
        if (!StringUtils.hasText(
                workspaceUid
        )) {
            throw new ValidationException(
                    "Workspace UID is required"
            );
        }

        return workspaceRepository
                .findByUid(
                        workspaceUid.trim()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace not found"
                        )
                );
    }

    private WorkspacePreviewPetDto toPetPreview(
            Pet pet
    ) {
        String mainImageUrl = null;

        if (pet.getImagesUrl() != null
                && !pet.getImagesUrl().isEmpty()) {

            mainImageUrl =
                    pet.getImagesUrl().get(0);
        }

        return WorkspacePreviewPetDto.builder()
                .pid(pet.getPid())
                .name(pet.getName())
                .mainImageUrl(
                        mainImageUrl
                )
                .build();
    }

    private String buildRestrictionMessage(
            WorkspaceStatus status
    ) {
        if (status == null) {
            return "Workspace status is unavailable";
        }

        return switch (status) {
            case ACTIVE ->
                    "Workspace is active";

            case PENDING_PAYMENT ->
                    "Complete the pending payment to activate this workspace";

            case PAST_DUE ->
                    "The latest payment could not be processed. Update payment to restore full access";

            case FROZEN ->
                    "This workspace is paused. Its data remains stored, but only a limited preview is available";

            case CANCELLED ->
                    "This workspace subscription is cancelled. Reactivate it to restore full access";
        };
    }
}