package com.dbp.uripet.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberRoleRequestDto {

    @NotBlank(
            message = "Role is required"
    )
    @Pattern(
            regexp = "^(ADMIN|MEMBER)$",
            message = "Role must be ADMIN or MEMBER"
    )
    private String role;
}