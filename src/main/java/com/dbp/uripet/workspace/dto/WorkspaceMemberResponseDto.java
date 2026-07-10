package com.dbp.uripet.workspace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class WorkspaceMemberResponseDto {
    private String workspaceUid;
    private String userUid;
    private String userName;
    private String userEmail;
    private String role;
    private boolean active;
    private ZonedDateTime createdAt;
}
