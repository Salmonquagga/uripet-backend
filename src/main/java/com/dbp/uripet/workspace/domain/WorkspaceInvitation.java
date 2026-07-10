package com.dbp.uripet.workspace.domain;

import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.workspace.domain.enums.WorkspaceInvitationStatus;
import com.dbp.uripet.workspace.domain.enums.WorkspaceRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "workspace_invitations",
        indexes = {
                @Index(
                        name = "idx_workspace_invitation_token",
                        columnList = "token"
                ),
                @Index(
                        name = "idx_workspace_invitation_email",
                        columnList = "invited_email"
                ),
                @Index(
                        name = "idx_workspace_invitation_workspace",
                        columnList = "workspace_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            unique = true,
            nullable = false,
            updatable = false
    )
    private String uid;

    @Column(
            unique = true,
            nullable = false,
            updatable = false
    )
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "workspace_id",
            nullable = false
    )
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "invited_by_id",
            nullable = false
    )
    private User invitedBy;

    @Column(
            name = "invited_email",
            nullable = false
    )
    private String invitedEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceInvitationStatus status;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private ZonedDateTime expiresAt;

    @Column(
            name = "responded_at"
    )
    private ZonedDateTime respondedAt;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();

        if (this.uid == null || this.uid.isBlank()) {
            this.uid = "INV-"
                    + UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase();
        }

        if (this.token == null || this.token.isBlank()) {
            this.token = UUID.randomUUID().toString();
        }

        if (this.role == null) {
            this.role = WorkspaceRole.MEMBER;
        }

        if (this.status == null) {
            this.status =
                    WorkspaceInvitationStatus.PENDING;
        }

        if (this.expiresAt == null) {
            this.expiresAt =
                    ZonedDateTime.now().plusDays(7);
        }
    }
}