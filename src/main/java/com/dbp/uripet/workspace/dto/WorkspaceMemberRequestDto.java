package com.dbp.uripet.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkspaceMemberRequestDto {

    @NotBlank(message = "User UID is required")
    private String userUid;

    private String role = "MEMBER";
}
