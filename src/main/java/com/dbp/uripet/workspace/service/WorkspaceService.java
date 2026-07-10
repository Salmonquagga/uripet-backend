package com.dbp.uripet.workspace.service;

import com.dbp.uripet.config.error.ConflictException;
import com.dbp.uripet.config.error.InvalidOperationException;
import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.config.error.ValidationException;
import com.dbp.uripet.pet.repository.PetRepository;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.user.repository.UserRepository;
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.WorkspaceMember;
import com.dbp.uripet.workspace.domain.enums.PlanType;
import com.dbp.uripet.workspace.domain.enums.WorkspaceRole;
import com.dbp.uripet.workspace.domain.enums.WorkspaceStatus;
import com.dbp.uripet.workspace.dto.TransferOwnershipRequestDto;
import com.dbp.uripet.workspace.dto.WorkspaceMemberRequestDto;
import com.dbp.uripet.workspace.dto.WorkspaceMemberResponseDto;
import com.dbp.uripet.workspace.dto.WorkspaceMemberRoleRequestDto;
import com.dbp.uripet.workspace.dto.WorkspacePermissionsDto;
import com.dbp.uripet.workspace.dto.WorkspaceRequestDto;
import com.dbp.uripet.workspace.dto.WorkspaceResponseDto;
import com.dbp.uripet.workspace.repository.WorkspaceMemberRepository;
import com.dbp.uripet.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    private final WorkspaceMemberRepository
            workspaceMemberRepository;

    private final UserRepository userRepository;

    private final PetRepository petRepository;

    private final PlanAccessService planAccessService;

    @Transactional
    public WorkspaceResponseDto
    createPersonalWorkspaceForUser(
            User user
    ) {
        return workspaceRepository
                .findByOwnerAndPlanType(
                        user,
                        PlanType.FREE
                )
                .map(workspace ->
                        ensurePersonalWorkspaceIsValid(
                                workspace,
                                user
                        )
                )
                .orElseGet(() -> {
                    Workspace workspace =
                            Workspace.builder()
                                    .name(
                                            "Mi espacio personal"
                                    )
                                    .owner(user)
                                    .planType(
                                            PlanType.FREE
                                    )
                                    .status(
                                            WorkspaceStatus.ACTIVE
                                    )
                                    .build();

                    workspaceRepository.save(workspace);

                    WorkspaceMember member =
                            WorkspaceMember.builder()
                                    .workspace(workspace)
                                    .user(user)
                                    .role(
                                            WorkspaceRole.OWNER
                                    )
                                    .active(true)
                                    .build();

                    workspaceMemberRepository.save(member);

                    return toResponse(
                            workspace,
                            user
                    );
                });
    }

    @Transactional
    public WorkspaceResponseDto createWorkspace(
            WorkspaceRequestDto request,
            User currentUser
    ) {
        throw new InvalidOperationException(
                "Paid workspaces must be created through billing checkout"
        );
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponseDto> getMyWorkspaces(
            User currentUser
    ) {
        return workspaceMemberRepository
                .findByUserAndActiveTrue(
                        currentUser
                )
                .stream()
                .map(
                        WorkspaceMember::getWorkspace
                )
                .distinct()
                .sorted(
                        workspaceComparator(
                                currentUser
                        )
                )
                .map(workspace ->
                        toResponse(
                                workspace,
                                currentUser
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponseDto getWorkspace(
            String workspaceUid,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspaceAndCheckMembership(
                        workspaceUid,
                        currentUser
                );

        return toResponse(
                workspace,
                currentUser
        );
    }

    @Transactional
    public WorkspaceResponseDto updateWorkspace(
            String workspaceUid,
            WorkspaceRequestDto request,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspaceAndCheckManageAccess(
                        workspaceUid,
                        currentUser
                );

        if (StringUtils.hasText(
                request.getName()
        )) {
            workspace.setName(
                    request.getName().trim()
            );
        }

        if (workspace.getPlanType()
                == PlanType.FREE) {

            workspace.setPlanType(
                    PlanType.FREE
            );

            workspace.setStatus(
                    WorkspaceStatus.ACTIVE
            );
        }

        return toResponse(
                workspaceRepository.save(
                        workspace
                ),
                currentUser
        );
    }

    @Transactional
    public void cancelWorkspace(
            String workspaceUid,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspaceAndCheckMembership(
                        workspaceUid,
                        currentUser
                );

        planAccessService.checkWorkspaceOwner(
                workspace,
                currentUser
        );

        if (workspace.getPlanType()
                == PlanType.FREE) {

            throw new InvalidOperationException(
                    "Personal workspace cannot be cancelled"
            );
        }

        throw new InvalidOperationException(
                "Cancel the workspace subscription through billing"
        );
    }

    @Transactional
    public WorkspaceMemberResponseDto addMember(
            String workspaceUid,
            WorkspaceMemberRequestDto request,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspaceAndCheckMembership(
                        workspaceUid,
                        currentUser
                );

        planAccessService.checkCanManageMembers(
                workspace,
                currentUser
        );

        User targetUser =
                userRepository
                        .findByUid(
                                request.getUserUid()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        WorkspaceMember existingMember =
                workspaceMemberRepository
                        .findByWorkspaceAndUser(
                                workspace,
                                targetUser
                        )
                        .orElse(null);

        if (existingMember != null
                && existingMember.isActive()) {

            throw new ConflictException(
                    "User already belongs to this workspace"
            );
        }

        WorkspaceRole role =
                parseAssignableRole(
                        request.getRole()
                );

        if (existingMember != null) {
            existingMember.setActive(true);
            existingMember.setRole(role);

            return toMemberResponse(
                    workspaceMemberRepository.save(
                            existingMember
                    )
            );
        }

        WorkspaceMember member =
                WorkspaceMember.builder()
                        .workspace(workspace)
                        .user(targetUser)
                        .role(role)
                        .active(true)
                        .build();

        return toMemberResponse(
                workspaceMemberRepository.save(
                        member
                )
        );
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponseDto>
    getMembers(
            String workspaceUid,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspaceAndCheckMembership(
                        workspaceUid,
                        currentUser
                );

        planAccessService.checkCanViewMembers(
                workspace,
                currentUser
        );

        return workspaceMemberRepository
                .findByWorkspaceAndActiveTrue(
                        workspace
                )
                .stream()
                .sorted(
                        Comparator.comparingInt(
                                member ->
                                        member
                                                .getRole()
                                                .ordinal()
                        )
                )
                .map(this::toMemberResponse)
                .toList();
    }

    @Transactional
    public WorkspaceMemberResponseDto
    updateMemberRole(
            String workspaceUid,
            String userUid,
            WorkspaceMemberRoleRequestDto request,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspaceAndCheckMembership(
                        workspaceUid,
                        currentUser
                );

        planAccessService.checkCanManageMembers(
                workspace,
                currentUser
        );

        WorkspaceMember currentMember =
                workspaceMemberRepository
                        .findByWorkspaceAndUserAndActiveTrue(
                                workspace,
                                currentUser
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Current workspace membership not found"
                                )
                        );

        User targetUser =
                getUser(userUid);

        WorkspaceMember targetMember =
                getActiveMember(
                        workspace,
                        targetUser
                );

        if (targetMember.getRole()
                == WorkspaceRole.OWNER) {

            throw new InvalidOperationException(
                    "Owner role cannot be changed through this endpoint"
            );
        }

        if (currentMember.getRole()
                == WorkspaceRole.ADMIN
                && targetMember.getRole()
                == WorkspaceRole.ADMIN) {

            throw new InvalidOperationException(
                    "An admin cannot change another admin role"
            );
        }

        WorkspaceRole newRole =
                parseAssignableRole(
                        request.getRole()
                );

        targetMember.setRole(newRole);

        return toMemberResponse(
                workspaceMemberRepository.save(
                        targetMember
                )
        );
    }

    @Transactional
    public void removeMember(
            String workspaceUid,
            String userUid,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspaceAndCheckMembership(
                        workspaceUid,
                        currentUser
                );

        planAccessService.checkCanManageMembers(
                workspace,
                currentUser
        );

        User targetUser =
                getUser(userUid);

        WorkspaceMember member =
                getActiveMember(
                        workspace,
                        targetUser
                );

        if (member.getRole()
                == WorkspaceRole.OWNER) {

            throw new InvalidOperationException(
                    "Cannot remove workspace owner"
            );
        }

        member.setActive(false);

        workspaceMemberRepository.save(member);
    }

    @Transactional
    public void leaveWorkspace(
            String workspaceUid,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspaceAndCheckMembership(
                        workspaceUid,
                        currentUser
                );

        WorkspaceMember membership =
                getActiveMember(
                        workspace,
                        currentUser
                );

        if (membership.getRole()
                == WorkspaceRole.OWNER) {

            throw new InvalidOperationException(
                    "Workspace owner must transfer ownership before leaving"
            );
        }

        membership.setActive(false);

        workspaceMemberRepository.save(
                membership
        );
    }

    @Transactional
    public WorkspaceResponseDto transferOwnership(
            String workspaceUid,
            TransferOwnershipRequestDto request,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspaceAndCheckMembership(
                        workspaceUid,
                        currentUser
                );

        planAccessService.checkWorkspaceOwner(
                workspace,
                currentUser
        );

        if (workspace.getPlanType()
                == PlanType.FREE) {

            throw new InvalidOperationException(
                    "Personal workspace ownership cannot be transferred"
            );
        }

        User newOwner =
                getUser(
                        request.getNewOwnerUserUid()
                );

        if (newOwner.getId()
                .equals(currentUser.getId())) {

            throw new InvalidOperationException(
                    "User is already the workspace owner"
            );
        }

        WorkspaceMember currentOwnerMember =
                getActiveMember(
                        workspace,
                        currentUser
                );

        WorkspaceMember newOwnerMember =
                getActiveMember(
                        workspace,
                        newOwner
                );

        currentOwnerMember.setRole(
                WorkspaceRole.ADMIN
        );

        newOwnerMember.setRole(
                WorkspaceRole.OWNER
        );

        workspaceMemberRepository.save(
                currentOwnerMember
        );

        workspaceMemberRepository.save(
                newOwnerMember
        );

        workspace.setOwner(newOwner);

        Workspace savedWorkspace =
                workspaceRepository.save(
                        workspace
                );

        return toResponse(
                savedWorkspace,
                newOwner
        );
    }

    private WorkspaceResponseDto
    ensurePersonalWorkspaceIsValid(
            Workspace workspace,
            User user
    ) {
        workspace.setOwner(user);
        workspace.setPlanType(
                PlanType.FREE
        );
        workspace.setStatus(
                WorkspaceStatus.ACTIVE
        );

        workspaceRepository.save(workspace);

        WorkspaceMember ownerMember =
                workspaceMemberRepository
                        .findByWorkspaceAndUser(
                                workspace,
                                user
                        )
                        .orElse(null);

        if (ownerMember == null) {
            ownerMember =
                    WorkspaceMember.builder()
                            .workspace(workspace)
                            .user(user)
                            .role(
                                    WorkspaceRole.OWNER
                            )
                            .active(true)
                            .build();
        } else {
            ownerMember.setRole(
                    WorkspaceRole.OWNER
            );
            ownerMember.setActive(true);
        }

        workspaceMemberRepository.save(
                ownerMember
        );

        return toResponse(
                workspace,
                user
        );
    }

    private Workspace
    getWorkspaceAndCheckMembership(
            String workspaceUid,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspace(workspaceUid);

        planAccessService.checkCanAccessWorkspace(
                workspace,
                currentUser
        );

        return workspace;
    }

    private Workspace
    getWorkspaceAndCheckManageAccess(
            String workspaceUid,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspaceAndCheckMembership(
                        workspaceUid,
                        currentUser
                );

        planAccessService.checkCanManageWorkspace(
                workspace,
                currentUser
        );

        return workspace;
    }

    private Workspace getWorkspace(
            String workspaceUid
    ) {
        if (!StringUtils.hasText(workspaceUid)) {
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

    private User getUser(
            String userUid
    ) {
        if (!StringUtils.hasText(userUid)) {
            throw new ValidationException(
                    "User UID is required"
            );
        }

        return userRepository
                .findByUid(
                        userUid.trim()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        )
                );
    }

    private WorkspaceMember getActiveMember(
            Workspace workspace,
            User user
    ) {
        return workspaceMemberRepository
                .findByWorkspaceAndUserAndActiveTrue(
                        workspace,
                        user
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Active workspace member not found"
                        )
                );
    }

    private WorkspaceRole parseAssignableRole(
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
                        "Use ownership transfer flow to assign OWNER role"
                );
            }

            return role;

        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "Role must be ADMIN or MEMBER"
            );
        }
    }

    private Comparator<Workspace>
    workspaceComparator(
            User currentUser
    ) {
        return Comparator
                .comparing(
                        (Workspace workspace) ->
                                workspace.getStatus()
                                        != WorkspaceStatus.ACTIVE
                )
                .thenComparing(workspace ->
                        workspace.getOwner() == null
                                || !workspace
                                .getOwner()
                                .getId()
                                .equals(
                                        currentUser.getId()
                                )
                )
                .thenComparing(workspace ->
                        workspace.getPlanType()
                                == PlanType.FREE
                                ? 0
                                : 1
                )
                .thenComparing(
                        Workspace::getName,
                        String.CASE_INSENSITIVE_ORDER
                );
    }

    private WorkspaceResponseDto toResponse(
            Workspace workspace,
            User currentUser
    ) {
        WorkspaceMember member =
                workspaceMemberRepository
                        .findByWorkspaceAndUserAndActiveTrue(
                                workspace,
                                currentUser
                        )
                        .orElse(null);

        long membersCount =
                workspaceMemberRepository
                        .countByWorkspaceAndActiveTrue(
                                workspace
                        );

        long petsCount =
                petRepository.countByWorkspace(
                        workspace
                );

        boolean owner =
                member != null
                        && member.getRole()
                        == WorkspaceRole.OWNER;

        boolean manager =
                member != null
                        && (
                        member.getRole()
                                == WorkspaceRole.OWNER
                                || member.getRole()
                                == WorkspaceRole.ADMIN
                );

        boolean personalWorkspace =
                workspace.getPlanType()
                        == PlanType.FREE;

        boolean active =
                workspace.getStatus()
                        == WorkspaceStatus.ACTIVE;

        WorkspacePermissionsDto permissions =
                planAccessService.buildPermissions(
                        workspace,
                        currentUser
                );

        return WorkspaceResponseDto.builder()
                .uid(workspace.getUid())
                .name(workspace.getName())
                .ownerUid(
                        workspace.getOwner() != null
                                ? workspace
                                  .getOwner()
                                  .getUid()
                                : null
                )
                .ownerName(
                        workspace.getOwner() != null
                                ? workspace
                                  .getOwner()
                                  .getName()
                                : null
                )
                .planType(
                        workspace
                                .getPlanType()
                                .name()
                )
                .status(
                        workspace
                                .getStatus()
                                .name()
                )
                .currentUserRole(
                        member != null
                                ? member
                                  .getRole()
                                  .name()
                                : null
                )
                .currentUserOwner(owner)
                .currentUserCanManage(manager)
                .personalWorkspace(
                        personalWorkspace
                )
                .active(active)
                .restricted(!active)
                .membersCount(
                        membersCount
                )
                .petsCount(
                        petsCount
                )
                .permissions(
                        permissions
                )
                .createdAt(
                        workspace.getCreatedAt()
                )
                .updatedAt(
                        workspace.getUpdatedAt()
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
                        member.getRole().name()
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