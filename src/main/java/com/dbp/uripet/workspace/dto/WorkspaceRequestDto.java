package com.dbp.uripet.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkspaceRequestDto {

    @NotBlank(message = "Workspace name is required")
    private String name;

    private String planType;
}
