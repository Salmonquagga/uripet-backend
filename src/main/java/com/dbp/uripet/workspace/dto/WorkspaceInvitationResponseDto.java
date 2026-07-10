package com.dbp.uripet.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceInvitationResponseDto {

    private String uid;

    private String workspaceUid;

    private String workspaceName;

    private String invitedEmail;

    private String invitedByUid;

    private String invitedByName;

    private String role;

    private String status;

    private boolean expired;

    private ZonedDateTime expiresAt;

    private ZonedDateTime respondedAt;

    private ZonedDateTime createdAt;
}