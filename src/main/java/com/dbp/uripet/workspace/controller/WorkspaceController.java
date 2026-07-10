package com.dbp.uripet.workspace.controller;

import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.workspace.dto.TransferOwnershipRequestDto;
import com.dbp.uripet.workspace.dto.WorkspaceMemberRequestDto;
import com.dbp.uripet.workspace.dto.WorkspaceMemberResponseDto;
import com.dbp.uripet.workspace.dto.WorkspaceMemberRoleRequestDto;
import com.dbp.uripet.workspace.dto.WorkspaceRequestDto;
import com.dbp.uripet.workspace.dto.WorkspaceResponseDto;
import com.dbp.uripet.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workspaces")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @GetMapping("/me")
    public ResponseEntity<List<WorkspaceResponseDto>>
    getMyWorkspaces(
            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                workspaceService.getMyWorkspaces(
                        currentUser
                )
        );
    }

    @GetMapping("/{workspaceUid}")
    public ResponseEntity<WorkspaceResponseDto>
    getWorkspace(
            @PathVariable
            String workspaceUid,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                workspaceService.getWorkspace(
                        workspaceUid,
                        currentUser
                )
        );
    }

    @PatchMapping("/{workspaceUid}")
    public ResponseEntity<WorkspaceResponseDto>
    updateWorkspace(
            @PathVariable
            String workspaceUid,

            @Valid
            @RequestBody
            WorkspaceRequestDto request,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                workspaceService.updateWorkspace(
                        workspaceUid,
                        request,
                        currentUser
                )
        );
    }

    @DeleteMapping("/{workspaceUid}")
    public ResponseEntity<Map<String, Object>>
    cancelWorkspace(
            @PathVariable
            String workspaceUid,

            @AuthenticationPrincipal
            User currentUser
    ) {
        workspaceService.cancelWorkspace(
                workspaceUid,
                currentUser
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Workspace subscription cancelled"
                )
        );
    }

    @GetMapping("/{workspaceUid}/members")
    public ResponseEntity<
            List<WorkspaceMemberResponseDto>
            >
    getMembers(
            @PathVariable
            String workspaceUid,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                workspaceService.getMembers(
                        workspaceUid,
                        currentUser
                )
        );
    }

    /*
     * Endpoint temporal.
     *
     * El flujo recomendado para el frontend será:
     * POST /workspaces/{workspaceUid}/invitations
     */
    @PostMapping("/{workspaceUid}/members")
    public ResponseEntity<WorkspaceMemberResponseDto>
    addMember(
            @PathVariable
            String workspaceUid,

            @Valid
            @RequestBody
            WorkspaceMemberRequestDto request,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return new ResponseEntity<>(
                workspaceService.addMember(
                        workspaceUid,
                        request,
                        currentUser
                ),
                HttpStatus.CREATED
        );
    }

    @PatchMapping(
            "/{workspaceUid}/members/{userUid}/role"
    )
    public ResponseEntity<WorkspaceMemberResponseDto>
    updateMemberRole(
            @PathVariable
            String workspaceUid,

            @PathVariable
            String userUid,

            @Valid
            @RequestBody
            WorkspaceMemberRoleRequestDto request,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                workspaceService.updateMemberRole(
                        workspaceUid,
                        userUid,
                        request,
                        currentUser
                )
        );
    }

    @DeleteMapping(
            "/{workspaceUid}/members/{userUid}"
    )
    public ResponseEntity<Map<String, Object>>
    removeMember(
            @PathVariable
            String workspaceUid,

            @PathVariable
            String userUid,

            @AuthenticationPrincipal
            User currentUser
    ) {
        workspaceService.removeMember(
                workspaceUid,
                userUid,
                currentUser
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Workspace member deactivated successfully"
                )
        );
    }

    @PostMapping("/{workspaceUid}/leave")
    public ResponseEntity<Map<String, Object>>
    leaveWorkspace(
            @PathVariable
            String workspaceUid,

            @AuthenticationPrincipal
            User currentUser
    ) {
        workspaceService.leaveWorkspace(
                workspaceUid,
                currentUser
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "You left the workspace successfully"
                )
        );
    }

    @PostMapping(
            "/{workspaceUid}/transfer-ownership"
    )
    public ResponseEntity<WorkspaceResponseDto>
    transferOwnership(
            @PathVariable
            String workspaceUid,

            @Valid
            @RequestBody
            TransferOwnershipRequestDto request,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                workspaceService.transferOwnership(
                        workspaceUid,
                        request,
                        currentUser
                )
        );
    }
}