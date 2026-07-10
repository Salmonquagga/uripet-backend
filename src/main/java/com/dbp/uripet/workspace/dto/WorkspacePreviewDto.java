package com.dbp.uripet.workspace.dto;

import com.dbp.uripet.workspace.domain.enums.PlanType;
import com.dbp.uripet.workspace.domain.enums.WorkspaceRole;
import com.dbp.uripet.workspace.domain.enums.WorkspaceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspacePreviewDto {

    private String workspaceUid;

    private String workspaceName;

    private PlanType planType;

    private WorkspaceStatus status;

    private WorkspaceRole currentUserRole;

    private boolean owner;

    private boolean active;

    private boolean restricted;

    private boolean canReactivate;

    private String restrictionMessage;

    private long petsCount;

    private List<WorkspacePreviewPetDto> pets;

    private PlanLimitsResponseDto limits;
}