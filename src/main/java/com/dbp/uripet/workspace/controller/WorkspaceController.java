package com.dbp.uripet.workspace.controller;

import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.workspace.dto.WorkspaceMemberRequestDto;
import com.dbp.uripet.workspace.dto.WorkspaceMemberResponseDto;
import com.dbp.uripet.workspace.dto.WorkspaceRequestDto;
import com.dbp.uripet.workspace.dto.WorkspaceResponseDto;
import com.dbp.uripet.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    /*
     * No existe POST /workspaces.
     *
     * Los grupos pagados se crearán desde:
     * POST /billing/checkout
     */

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

    /*
     * Se mantiene temporalmente para devolver
     * un mensaje orientativo.
     *
     * No cancela ni elimina el grupo.
     */
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
     * Este endpoint todavía usa userUid.
     *
     * En el bloque de invitaciones lo reemplazaremos
     * por invitaciones mediante correo.
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
        return ResponseEntity.ok(
                workspaceService.addMember(
                        workspaceUid,
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
}