package com.dbp.uripet.workspace.controller;

import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.workspace.dto.WorkspacePreviewDto;
import com.dbp.uripet.workspace.service.WorkspacePreviewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WorkspacePreviewController {

    private final WorkspacePreviewService
            workspacePreviewService;

    @GetMapping(
            "/workspaces/{workspaceUid}/preview"
    )
    public ResponseEntity<WorkspacePreviewDto>
    getWorkspacePreview(
            @PathVariable
            String workspaceUid,

            @AuthenticationPrincipal
            User currentUser
    ) {
        return ResponseEntity.ok(
                workspacePreviewService
                        .getPreview(
                                workspaceUid,
                                currentUser
                        )
        );
    }
}