package com.dbp.uripet.workspace.repository;

import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.WorkspaceInvitation;
import com.dbp.uripet.workspace.domain.enums.WorkspaceInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceInvitationRepository
        extends JpaRepository<WorkspaceInvitation, Long> {

    Optional<WorkspaceInvitation> findByUid(
            String uid
    );

    Optional<WorkspaceInvitation> findByToken(
            String token
    );

    Optional<WorkspaceInvitation>
    findByWorkspaceAndInvitedEmailIgnoreCaseAndStatus(
            Workspace workspace,
            String invitedEmail,
            WorkspaceInvitationStatus status
    );

    boolean
    existsByWorkspaceAndInvitedEmailIgnoreCaseAndStatus(
            Workspace workspace,
            String invitedEmail,
            WorkspaceInvitationStatus status
    );

    List<WorkspaceInvitation>
    findByWorkspaceOrderByCreatedAtDesc(
            Workspace workspace
    );

    List<WorkspaceInvitation>
    findByInvitedEmailIgnoreCaseOrderByCreatedAtDesc(
            String invitedEmail
    );

    List<WorkspaceInvitation>
    findByInvitedEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
            String invitedEmail,
            WorkspaceInvitationStatus status
    );
}