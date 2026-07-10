package com.dbp.uripet.workspace.service;

import com.dbp.uripet.config.error.ConflictException;
import com.dbp.uripet.config.error.ForbiddenException;
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
import com.dbp.uripet.workspace.dto.WorkspaceMemberRequestDto;
import com.dbp.uripet.workspace.dto.WorkspaceMemberResponseDto;
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

    /*
     * Se ejecuta automáticamente al registrar
     * a un usuario.
     */
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

    /*
     * Ya no se permite crear grupos pagados
     * directamente desde WorkspaceService.
     *
     * El grupo se creará desde BillingService
     * junto con su suscripción y transacción.
     */
    @Transactional
    public WorkspaceResponseDto createWorkspace(
            WorkspaceRequestDto request,
            User currentUser
    ) {
        throw new InvalidOperationException(
                "Paid workspaces must be created through billing checkout"
        );
    }

    /*
     * Lista únicamente las membresías activas.
     *
     * Un registro inactivo no debe permitir acceso,
     * aunque siga guardado para historial.
     */
    @Transactional(readOnly = true)
    public List<WorkspaceResponseDto> getMyWorkspaces(
            User currentUser
    ) {
        return workspaceMemberRepository
                .findByUserAndActiveTrue(currentUser)
                .stream()
                .map(WorkspaceMember::getWorkspace)
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

    /*
     * Solo modifica el nombre.
     *
     * El plan y el estado nunca se reciben
     * desde el frontend por este endpoint.
     */
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

        /*
         * Protección adicional para el espacio personal.
         */
        if (workspace.getPlanType()
                == PlanType.FREE) {

            workspace.setPlanType(
                    PlanType.FREE
            );

            workspace.setStatus(
                    WorkspaceStatus.ACTIVE
            );
        }

        Workspace savedWorkspace =
                workspaceRepository.save(workspace);

        return toResponse(
                savedWorkspace,
                currentUser
        );
    }

    /*
     * No elimina físicamente el grupo.
     *
     * La cancelación comercial real se realizará
     * desde BillingService.
     */
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

        /*
         * Si ya pertenecía y está activo,
         * no se vuelve a crear.
         */
        if (existingMember != null
                && existingMember.isActive()) {

            throw new ConflictException(
                    "User already belongs to this workspace"
            );
        }

        WorkspaceRole role =
                parseWorkspaceRole(
                        request.getRole()
                );

        if (role == WorkspaceRole.OWNER) {
            throw new InvalidOperationException(
                    "Use ownership transfer flow to assign OWNER role"
            );
        }

        /*
         * Si existía una membresía inactiva,
         * se reactiva en vez de insertar otro registro
         * y romper la restricción única.
         */
        if (existingMember != null) {
            existingMember.setActive(true);
            existingMember.setRole(role);

            WorkspaceMember savedMember =
                    workspaceMemberRepository.save(
                            existingMember
                    );

            return toMemberResponse(
                    savedMember
            );
        }

        WorkspaceMember member =
                WorkspaceMember.builder()
                        .workspace(workspace)
                        .user(targetUser)
                        .role(role)
                        .active(true)
                        .build();

        WorkspaceMember savedMember =
                workspaceMemberRepository.save(
                        member
                );

        return toMemberResponse(savedMember);
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
                        Comparator.comparing(
                                member ->
                                        member.getRole()
                                                .ordinal()
                        )
                )
                .map(this::toMemberResponse)
                .toList();
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
                userRepository
                        .findByUid(userUid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        WorkspaceMember member =
                workspaceMemberRepository
                        .findByWorkspaceAndUserAndActiveTrue(
                                workspace,
                                targetUser
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Active workspace member not found"
                                )
                        );

        if (member.getRole()
                == WorkspaceRole.OWNER) {

            throw new InvalidOperationException(
                    "Cannot remove workspace owner"
            );
        }

        /*
         * No borramos físicamente.
         * Se conserva como historial y queda sin acceso.
         */
        member.setActive(false);

        workspaceMemberRepository.save(member);
    }

    private WorkspaceResponseDto
    ensurePersonalWorkspaceIsValid(
            Workspace workspace,
            User user
    ) {
        workspace.setOwner(user);
        workspace.setPlanType(PlanType.FREE);
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

        workspaceMemberRepository.save(ownerMember);

        return toResponse(workspace, user);
    }

    private Workspace getWorkspaceAndCheckMembership(
            String workspaceUid,
            User currentUser
    ) {
        Workspace workspace =
                workspaceRepository
                        .findByUid(workspaceUid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Workspace not found"
                                )
                        );

        planAccessService.checkCanAccessWorkspace(
                workspace,
                currentUser
        );

        return workspace;
    }

    private Workspace getWorkspaceAndCheckManageAccess(
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

    private WorkspaceRole parseWorkspaceRole(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return WorkspaceRole.MEMBER;
        }

        try {
            return WorkspaceRole.valueOf(
                    value.trim().toUpperCase()
            );
        } catch (Exception exception) {
            throw new ValidationException(
                    "Invalid workspace role: "
                            + value
            );
        }
    }

    private Comparator<Workspace>
    workspaceComparator(
            User currentUser
    ) {
        return Comparator
                /*
                 * Activos primero.
                 */
                .comparing(
                        (Workspace workspace) ->
                                workspace.getStatus()
                                        != WorkspaceStatus.ACTIVE
                )
                /*
                 * Grupos propios antes que grupos ajenos.
                 */
                .thenComparing(workspace ->
                        workspace.getOwner() == null
                                || !workspace
                                .getOwner()
                                .getId()
                                .equals(
                                        currentUser.getId()
                                )
                )
                /*
                 * Espacio personal primero
                 * entre los espacios propios.
                 */
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
                        workspace.getPlanType().name()
                )
                .status(
                        workspace.getStatus().name()
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
                .membersCount(membersCount)
                .petsCount(petsCount)
                .permissions(permissions)
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
        User user = member.getUser();

        Workspace workspace =
                member.getWorkspace();

        return WorkspaceMemberResponseDto.builder()
                .workspaceUid(
                        workspace.getUid()
                )
                .userUid(user.getUid())
                .userName(user.getName())
                .userEmail(user.getEmail())
                .role(member.getRole().name())
                .active(member.isActive())
                .createdAt(
                        member.getCreatedAt()
                )
                .build();
    }
}