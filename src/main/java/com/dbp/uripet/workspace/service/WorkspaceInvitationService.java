package com.dbp.uripet.workspace.service;

import com.dbp.uripet.config.error.ConflictException;
import com.dbp.uripet.config.error.ForbiddenException;
import com.dbp.uripet.config.error.InvalidOperationException;
import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.config.error.ValidationException;
import com.dbp.uripet.events.email.EmailService;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.user.repository.UserRepository;
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.WorkspaceInvitation;
import com.dbp.uripet.workspace.domain.WorkspaceMember;
import com.dbp.uripet.workspace.domain.enums.PlanType;
import com.dbp.uripet.workspace.domain.enums.WorkspaceInvitationStatus;
import com.dbp.uripet.workspace.domain.enums.WorkspaceRole;
import com.dbp.uripet.workspace.domain.enums.WorkspaceStatus;
import com.dbp.uripet.workspace.dto.WorkspaceInvitationRequestDto;
import com.dbp.uripet.workspace.dto.WorkspaceInvitationResponseDto;
import com.dbp.uripet.workspace.dto.WorkspaceMemberResponseDto;
import com.dbp.uripet.workspace.repository.WorkspaceInvitationRepository;
import com.dbp.uripet.workspace.repository.WorkspaceMemberRepository;
import com.dbp.uripet.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceInvitationService {

    private final WorkspaceRepository
            workspaceRepository;

    private final WorkspaceInvitationRepository
            workspaceInvitationRepository;

    private final WorkspaceMemberRepository
            workspaceMemberRepository;

    private final UserRepository userRepository;

    private final PlanAccessService
            planAccessService;

    private final EmailService emailService;

    @Transactional
    public WorkspaceInvitationResponseDto
    createInvitation(
            String workspaceUid,
            WorkspaceInvitationRequestDto request,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspace(workspaceUid);

        planAccessService.checkCanManageMembers(
                workspace,
                currentUser
        );

        if (workspace.getStatus()
                != WorkspaceStatus.ACTIVE) {

            throw new InvalidOperationException(
                    "Workspace must be active to create invitations"
            );
        }

        if (workspace.getPlanType()
                == PlanType.FREE) {

            throw new InvalidOperationException(
                    "Free workspace cannot invite members"
            );
        }

        String email =
                normalizeEmail(
                        request.getEmail()
                );

        if (email.equalsIgnoreCase(
                currentUser.getEmail()
        )) {
            throw new InvalidOperationException(
                    "You cannot invite yourself"
            );
        }

        User invitedUser =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "A registered UriPet user with this email was not found"
                                )
                        );

        if (!invitedUser.isVerified()) {
            throw new InvalidOperationException(
                    "Invited user must verify their account first"
            );
        }

        if (workspaceMemberRepository
                .existsByWorkspaceAndUserAndActiveTrue(
                        workspace,
                        invitedUser
                )) {

            throw new ConflictException(
                    "User already belongs to this workspace"
            );
        }

        expirePreviousInvitationIfNecessary(
                workspace,
                email
        );

        if (workspaceInvitationRepository
                .existsByWorkspaceAndInvitedEmailIgnoreCaseAndStatus(
                        workspace,
                        email,
                        WorkspaceInvitationStatus.PENDING
                )) {

            throw new ConflictException(
                    "A pending invitation already exists for this email"
            );
        }

        WorkspaceRole role =
                parseInvitableRole(
                        request.getRole()
                );

        WorkspaceInvitation invitation =
                WorkspaceInvitation.builder()
                        .workspace(workspace)
                        .invitedBy(currentUser)
                        .invitedEmail(email)
                        .role(role)
                        .status(
                                WorkspaceInvitationStatus.PENDING
                        )
                        .expiresAt(
                                ZonedDateTime.now()
                                        .plusDays(7)
                        )
                        .build();

        WorkspaceInvitation savedInvitation =
                workspaceInvitationRepository.save(
                        invitation
                );

        sendInvitationEmail(
                savedInvitation
        );

        return toResponse(
                savedInvitation
        );
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponseDto>
    getWorkspaceInvitations(
            String workspaceUid,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspace(workspaceUid);

        planAccessService.checkCanManageWorkspace(
                workspace,
                currentUser
        );

        return workspaceInvitationRepository
                .findByWorkspaceOrderByCreatedAtDesc(
                        workspace
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WorkspaceInvitationResponseDto>
    getMyInvitations(
            User currentUser
    ) {
        return workspaceInvitationRepository
                .findByInvitedEmailIgnoreCaseOrderByCreatedAtDesc(
                        currentUser.getEmail()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WorkspaceMemberResponseDto
    acceptInvitation(
            String token,
            User currentUser
    ) {
        WorkspaceInvitation invitation =
                findInvitationByToken(
                        token
                );

        validateInvitationForUser(
                invitation,
                currentUser
        );

        Workspace workspace =
                invitation.getWorkspace();

        if (workspace.getStatus()
                != WorkspaceStatus.ACTIVE) {

            throw new InvalidOperationException(
                    "Workspace is not active"
            );
        }

        /*
         * CAMBIO DEL BLOQUE 8:
         *
         * La validación del límite se hace
         * al aceptar la invitación.
         *
         * Una invitación pendiente todavía
         * no ocupa un espacio dentro del plan.
         */
        boolean alreadyActiveMember =
                workspaceMemberRepository
                        .existsByWorkspaceAndUserAndActiveTrue(
                                workspace,
                                currentUser
                        );

        if (!alreadyActiveMember) {
            planAccessService.checkMemberLimit(
                    workspace
            );
        }

        WorkspaceMember member =
                workspaceMemberRepository
                        .findByWorkspaceAndUser(
                                workspace,
                                currentUser
                        )
                        .orElse(null);

        if (member != null
                && member.isActive()) {

            invitation.setStatus(
                    WorkspaceInvitationStatus.ACCEPTED
            );

            invitation.setRespondedAt(
                    ZonedDateTime.now()
            );

            workspaceInvitationRepository.save(
                    invitation
            );

            return toMemberResponse(
                    member
            );
        }

        if (member == null) {
            member =
                    WorkspaceMember.builder()
                            .workspace(workspace)
                            .user(currentUser)
                            .role(
                                    invitation.getRole()
                            )
                            .active(true)
                            .build();
        } else {
            member.setRole(
                    invitation.getRole()
            );

            member.setActive(true);
        }

        WorkspaceMember savedMember =
                workspaceMemberRepository.save(
                        member
                );

        invitation.setStatus(
                WorkspaceInvitationStatus.ACCEPTED
        );

        invitation.setRespondedAt(
                ZonedDateTime.now()
        );

        workspaceInvitationRepository.save(
                invitation
        );

        return toMemberResponse(
                savedMember
        );
    }

    @Transactional
    public WorkspaceInvitationResponseDto
    rejectInvitation(
            String token,
            User currentUser
    ) {
        WorkspaceInvitation invitation =
                findInvitationByToken(
                        token
                );

        validateInvitationForUser(
                invitation,
                currentUser
        );

        invitation.setStatus(
                WorkspaceInvitationStatus.REJECTED
        );

        invitation.setRespondedAt(
                ZonedDateTime.now()
        );

        WorkspaceInvitation savedInvitation =
                workspaceInvitationRepository.save(
                        invitation
                );

        return toResponse(
                savedInvitation
        );
    }

    @Transactional
    public WorkspaceInvitationResponseDto
    cancelInvitation(
            String workspaceUid,
            String invitationUid,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspace(workspaceUid);

        planAccessService.checkCanManageMembers(
                workspace,
                currentUser
        );

        WorkspaceInvitation invitation =
                workspaceInvitationRepository
                        .findByUid(
                                invitationUid
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Workspace invitation not found"
                                )
                        );

        if (!invitation
                .getWorkspace()
                .getId()
                .equals(
                        workspace.getId()
                )) {

            throw new ForbiddenException(
                    "Invitation does not belong to this workspace"
            );
        }

        if (invitation.getStatus()
                != WorkspaceInvitationStatus.PENDING) {

            throw new InvalidOperationException(
                    "Only pending invitations can be cancelled"
            );
        }

        invitation.setStatus(
                WorkspaceInvitationStatus.CANCELLED
        );

        invitation.setRespondedAt(
                ZonedDateTime.now()
        );

        return toResponse(
                workspaceInvitationRepository.save(
                        invitation
                )
        );
    }

    private void
    expirePreviousInvitationIfNecessary(
            Workspace workspace,
            String email
    ) {
        WorkspaceInvitation invitation =
                workspaceInvitationRepository
                        .findByWorkspaceAndInvitedEmailIgnoreCaseAndStatus(
                                workspace,
                                email,
                                WorkspaceInvitationStatus.PENDING
                        )
                        .orElse(null);

        if (invitation == null) {
            return;
        }

        if (isExpired(invitation)) {
            invitation.setStatus(
                    WorkspaceInvitationStatus.EXPIRED
            );

            invitation.setRespondedAt(
                    ZonedDateTime.now()
            );

            workspaceInvitationRepository.save(
                    invitation
            );
        }
    }

    private void validateInvitationForUser(
            WorkspaceInvitation invitation,
            User currentUser
    ) {
        if (!invitation
                .getInvitedEmail()
                .equalsIgnoreCase(
                        currentUser.getEmail()
                )) {

            throw new ForbiddenException(
                    "Invitation does not belong to the authenticated user"
            );
        }

        if (invitation.getStatus()
                != WorkspaceInvitationStatus.PENDING) {

            throw new InvalidOperationException(
                    "Invitation is no longer pending"
            );
        }

        if (isExpired(invitation)) {
            invitation.setStatus(
                    WorkspaceInvitationStatus.EXPIRED
            );

            invitation.setRespondedAt(
                    ZonedDateTime.now()
            );

            workspaceInvitationRepository.save(
                    invitation
            );

            throw new InvalidOperationException(
                    "Invitation has expired"
            );
        }
    }

    private WorkspaceInvitation
    findInvitationByToken(
            String token
    ) {
        if (!StringUtils.hasText(token)) {
            throw new ValidationException(
                    "Invitation token is required"
            );
        }

        return workspaceInvitationRepository
                .findByToken(
                        token.trim()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace invitation not found"
                        )
                );
    }

    private Workspace getWorkspace(
            String workspaceUid
    ) {
        if (!StringUtils.hasText(
                workspaceUid
        )) {
            throw new ValidationException(
                    "Workspace UID is required"
            );
        }

        return workspaceRepository
                .findByUid(
                        workspaceUid.trim()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace not found"
                        )
                );
    }

    private WorkspaceRole parseInvitableRole(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return WorkspaceRole.MEMBER;
        }

        try {
            WorkspaceRole role =
                    WorkspaceRole.valueOf(
                            value.trim()
                                    .toUpperCase()
                    );

            if (role == WorkspaceRole.OWNER) {
                throw new InvalidOperationException(
                        "OWNER role cannot be assigned through an invitation"
                );
            }

            return role;

        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "Invitation role must be ADMIN or MEMBER"
            );
        }
    }

    private String normalizeEmail(
            String email
    ) {
        if (!StringUtils.hasText(email)) {
            throw new ValidationException(
                    "Email is required"
            );
        }

        return email
                .trim()
                .toLowerCase();
    }

    private boolean isExpired(
            WorkspaceInvitation invitation
    ) {
        return invitation.getExpiresAt() != null
                && invitation
                .getExpiresAt()
                .isBefore(
                        ZonedDateTime.now()
                );
    }

    private void sendInvitationEmail(
            WorkspaceInvitation invitation
    ) {
        String subject =
                "Invitación a "
                        + invitation
                        .getWorkspace()
                        .getName()
                        + " en UriPet";

        String body =
                "Has sido invitado al grupo \""
                        + invitation
                        .getWorkspace()
                        .getName()
                        + "\" en UriPet.\n\n"
                        + "Rol: "
                        + invitation.getRole().name()
                        + "\n"
                        + "Token de invitación: "
                        + invitation.getToken()
                        + "\n"
                        + "La invitación vence el: "
                        + invitation.getExpiresAt()
                        + "\n\n"
                        + "Ingresa a UriPet para aceptar o rechazar la invitación.";

        emailService.sendEmail(
                invitation.getInvitedEmail(),
                subject,
                body
        );
    }

    private WorkspaceInvitationResponseDto
    toResponse(
            WorkspaceInvitation invitation
    ) {
        return WorkspaceInvitationResponseDto.builder()
                .uid(
                        invitation.getUid()
                )
                .workspaceUid(
                        invitation
                                .getWorkspace()
                                .getUid()
                )
                .workspaceName(
                        invitation
                                .getWorkspace()
                                .getName()
                )
                .invitedEmail(
                        invitation
                                .getInvitedEmail()
                )
                .invitedByUid(
                        invitation
                                .getInvitedBy()
                                .getUid()
                )
                .invitedByName(
                        invitation
                                .getInvitedBy()
                                .getName()
                )
                .role(
                        invitation
                                .getRole()
                                .name()
                )
                .status(
                        invitation
                                .getStatus()
                                .name()
                )
                .expired(
                        isExpired(invitation)
                )
                .expiresAt(
                        invitation.getExpiresAt()
                )
                .respondedAt(
                        invitation.getRespondedAt()
                )
                .createdAt(
                        invitation.getCreatedAt()
                )
                .build();
    }

    private WorkspaceMemberResponseDto
    toMemberResponse(
            WorkspaceMember member
    ) {
        return WorkspaceMemberResponseDto.builder()
                .workspaceUid(
                        member
                                .getWorkspace()
                                .getUid()
                )
                .userUid(
                        member
                                .getUser()
                                .getUid()
                )
                .userName(
                        member
                                .getUser()
                                .getName()
                )
                .userEmail(
                        member
                                .getUser()
                                .getEmail()
                )
                .role(
                        member
                                .getRole()
                                .name()
                )
                .active(
                        member.isActive()
                )
                .createdAt(
                        member.getCreatedAt()
                )
                .build();
    }
}