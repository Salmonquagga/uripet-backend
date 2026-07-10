package com.dbp.uripet.workspace.dto;

import jakarta.validation.constraints.Email;
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
public class WorkspaceInvitationRequestDto {

    @NotBlank(
            message = "Email is required"
    )
    @Email(
            message = "Email must be valid"
    )
    private String email;

    @Pattern(
            regexp = "^(ADMIN|MEMBER)$",
            message = "Role must be ADMIN or MEMBER"
    )
    @Builder.Default
    private String role = "MEMBER";
}