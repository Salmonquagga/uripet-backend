package com.dbp.uripet.workspace.controller;

import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.workspace.dto.WorkspaceInvitationRequestDto;
import com.dbp.uripet.workspace.dto.WorkspaceInvitationResponseDto;
import com.dbp.uripet.workspace.dto.WorkspaceMemberResponseDto;
import com.dbp.uripet.workspace.service.WorkspaceInvitationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService
            workspaceInvitationService;

    @PostMapping(
            "/workspaces/{workspaceUid}/invitations"
    )
    public ResponseEntity<WorkspaceInvitationResponseDto>
    createInvitation(
            @PathVariable
            String workspaceUid,

            @Valid
            @RequestBody
            WorkspaceInvitationRequestDto request,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return new ResponseEntity<>(
                workspaceInvitationService
                        .createInvitation(
                                workspaceUid,
                                request,
                                currentUser
                        ),
                HttpStatus.CREATED
        );
    }

    @GetMapping(
            "/workspaces/{workspaceUid}/invitations"
    )
    public ResponseEntity<
            List<WorkspaceInvitationResponseDto>
            >
    getWorkspaceInvitations(
            @PathVariable
            String workspaceUid,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                workspaceInvitationService
                        .getWorkspaceInvitations(
                                workspaceUid,
                                currentUser
                        )
        );
    }

    @DeleteMapping(
            "/workspaces/{workspaceUid}/invitations/{invitationUid}"
    )
    public ResponseEntity<WorkspaceInvitationResponseDto>
    cancelInvitation(
            @PathVariable
            String workspaceUid,

            @PathVariable
            String invitationUid,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                workspaceInvitationService
                        .cancelInvitation(
                                workspaceUid,
                                invitationUid,
                                currentUser
                        )
        );
    }

    @GetMapping(
            "/workspace-invitations/me"
    )
    public ResponseEntity<
            List<WorkspaceInvitationResponseDto>
            >
    getMyInvitations(
            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                workspaceInvitationService
                        .getMyInvitations(
                                currentUser
                        )
        );
    }

    @PostMapping(
            "/workspace-invitations/{token}/accept"
    )
    public ResponseEntity<WorkspaceMemberResponseDto>
    acceptInvitation(
            @PathVariable
            String token,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                workspaceInvitationService
                        .acceptInvitation(
                                token,
                                currentUser
                        )
        );
    }

    @PostMapping(
            "/workspace-invitations/{token}/reject"
    )
    public ResponseEntity<WorkspaceInvitationResponseDto>
    rejectInvitation(
            @PathVariable
            String token,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                workspaceInvitationService
                        .rejectInvitation(
                                token,
                                currentUser
                        )
        );
    }
}