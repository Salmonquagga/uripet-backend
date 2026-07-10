package com.dbp.uripet.workspace.service;

import com.dbp.uripet.config.error.ForbiddenException;
import com.dbp.uripet.config.error.InvalidOperationException;
import com.dbp.uripet.pet.repository.PetRepository;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.WorkspaceMember;
import com.dbp.uripet.workspace.domain.enums.PlanFeature;
import com.dbp.uripet.workspace.domain.enums.PlanType;
import com.dbp.uripet.workspace.domain.enums.WorkspaceRole;
import com.dbp.uripet.workspace.domain.enums.WorkspaceStatus;
import com.dbp.uripet.workspace.dto.WorkspacePermissionsDto;
import com.dbp.uripet.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlanAccessService {

    private final WorkspaceMemberRepository
            workspaceMemberRepository;

    private final PetRepository petRepository;

    /*
     * Verifica que el usuario tenga una membresía activa.
     */
    public WorkspaceMember checkCanAccessWorkspace(
            Workspace workspace,
            User user
    ) {
        if (workspace == null) {
            throw new ForbiddenException(
                    "Workspace is required"
            );
        }

        if (user == null) {
            throw new ForbiddenException(
                    "Authenticated user is required"
            );
        }

        return workspaceMemberRepository
                .findByWorkspaceAndUserAndActiveTrue(
                        workspace,
                        user
                )
                .orElseThrow(() ->
                        new ForbiddenException(
                                "Not authorized to access this workspace"
                        )
                );
    }

    /*
     * OWNER y ADMIN pueden administrar contenido.
     */
    public WorkspaceMember checkCanManageWorkspace(
            Workspace workspace,
            User user
    ) {
        WorkspaceMember member =
                checkCanAccessWorkspace(workspace, user);

        if (member.getRole() != WorkspaceRole.OWNER
                && member.getRole()
                != WorkspaceRole.ADMIN) {

            throw new ForbiddenException(
                    "Owner or admin role required"
            );
        }

        return member;
    }

    /*
     * Solamente OWNER administra pagos,
     * suscripción, cancelación y reactivación.
     */
    public WorkspaceMember checkWorkspaceOwner(
            Workspace workspace,
            User user
    ) {
        WorkspaceMember member =
                checkCanAccessWorkspace(workspace, user);

        if (member.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException(
                    "Workspace owner role required"
            );
        }

        return member;
    }

    public void checkWorkspaceActive(
            Workspace workspace
    ) {
        if (workspace == null
                || workspace.getStatus()
                != WorkspaceStatus.ACTIVE) {

            throw new ForbiddenException(
                    "Workspace is not active"
            );
        }
    }

    /*
     * Mascotas.
     */
    public void checkCanViewPets(
            Workspace workspace,
            User user
    ) {
        checkCanAccessWorkspace(workspace, user);
    }

    public void checkCanCreatePet(
            Workspace workspace,
            User user
    ) {
        checkWorkspaceActive(workspace);
        checkCanManageWorkspace(workspace, user);
        checkPetLimit(workspace);
    }

    public void checkCanEditPet(
            Workspace workspace,
            User user
    ) {
        checkWorkspaceActive(workspace);
        checkCanManageWorkspace(workspace, user);
    }

    public void checkCanDeletePet(
            Workspace workspace,
            User user
    ) {
        checkWorkspaceActive(workspace);
        checkCanManageWorkspace(workspace, user);
    }

    /*
     * Historial médico.
     */
    public void checkCanViewHealthRecords(
            Workspace workspace,
            User user
    ) {
        checkCanAccessWorkspace(workspace, user);
        checkWorkspaceActive(workspace);
    }

    public void checkCanManageHealthRecords(
            Workspace workspace,
            User user
    ) {
        checkWorkspaceActive(workspace);
        checkCanManageWorkspace(workspace, user);
    }

    /*
     * Miembros.
     */
    public void checkCanViewMembers(
            Workspace workspace,
            User user
    ) {
        checkCanAccessWorkspace(workspace, user);
    }

    public void checkCanManageMembers(
            Workspace workspace,
            User user
    ) {
        checkWorkspaceActive(workspace);
        checkCanManageWorkspace(workspace, user);

        if (workspace.getPlanType() == PlanType.FREE) {
            throw new InvalidOperationException(
                    "Free workspace cannot have additional members"
            );
        }
    }

    /*
     * Facturación.
     */
    public void checkCanManageBilling(
            Workspace workspace,
            User user
    ) {
        checkWorkspaceOwner(workspace, user);

        if (workspace.getPlanType() == PlanType.FREE) {
            throw new InvalidOperationException(
                    "Free workspace does not have billing"
            );
        }
    }

    /*
     * Límite conocido y definitivo del plan gratuito.
     *
     * Los límites de FAMILY y PREMIUM se agregarán
     * cuando fijemos sus cantidades comerciales.
     */
    public void checkPetLimit(
            Workspace workspace
    ) {
        if (workspace.getPlanType() == PlanType.FREE
                && petRepository.countByWorkspace(workspace)
                >= 1) {

            throw new InvalidOperationException(
                    "Free workspace can only have one pet"
            );
        }
    }

    /*
     * Funciones por plan.
     */
    public void checkFeature(
            Workspace workspace,
            PlanFeature feature
    ) {
        if (!hasFeature(workspace, feature)) {
            throw new ForbiddenException(
                    "Feature "
                            + feature.name()
                            + " is not available for the current plan"
            );
        }
    }

    public boolean hasFeature(
            Workspace workspace,
            PlanFeature feature
    ) {
        if (workspace == null
                || workspace.getPlanType() == null
                || feature == null) {

            return false;
        }

        PlanType planType = workspace.getPlanType();

        return switch (feature) {
            /*
             * QR básico y privacidad disponibles
             * para todos.
             */
            case BASIC_QR,
                 QR_PRIVACY_SETTINGS -> true;

            /*
             * Funciones avanzadas.
             */
            case PUBLIC_HEALTH_SUMMARY,
                 CUSTOM_QR_COLORS,
                 CUSTOM_QR_STYLE,
                 CUSTOM_QR_LOGO,
                 PDF_EXPORT,
                 REMINDERS ->
                    planType == PlanType.PREMIUM
                            || planType
                            == PlanType.VETERINARY
                            || planType
                            == PlanType.SHELTER;
        };
    }

    /*
     * Permisos que consumirá directamente el frontend.
     */
    public WorkspacePermissionsDto buildPermissions(
            Workspace workspace,
            User user
    ) {
        WorkspaceMember member =
                workspaceMemberRepository
                        .findByWorkspaceAndUserAndActiveTrue(
                                workspace,
                                user
                        )
                        .orElse(null);

        if (member == null) {
            return noPermissions();
        }

        boolean active =
                workspace.getStatus()
                        == WorkspaceStatus.ACTIVE;

        boolean owner =
                member.getRole()
                        == WorkspaceRole.OWNER;

        boolean admin =
                member.getRole()
                        == WorkspaceRole.ADMIN;

        boolean manager = owner || admin;

        boolean free =
                workspace.getPlanType()
                        == PlanType.FREE;

        boolean mayReactivate =
                owner
                        && !free
                        && (
                        workspace.getStatus()
                                == WorkspaceStatus.FROZEN
                                || workspace.getStatus()
                                == WorkspaceStatus.PAST_DUE
                                || workspace.getStatus()
                                == WorkspaceStatus.CANCELLED
                                || workspace.getStatus()
                                == WorkspaceStatus.PENDING_PAYMENT
                );

        return WorkspacePermissionsDto.builder()
                .canAccessWorkspace(true)
                .canViewPreview(true)

                .canViewPets(true)
                .canCreatePets(active && manager)
                .canEditPets(active && manager)
                .canDeletePets(active && manager)

                .canViewHealthRecords(active)
                .canManageHealthRecords(
                        active && manager
                )

                .canViewMembers(true)
                .canManageMembers(
                        active && manager && !free
                )

                .canManageBilling(owner && !free)

                .canRenameWorkspace(manager)
                .canCancelSubscription(
                        active && owner && !free
                )
                .canReactivateSubscription(
                        mayReactivate
                )

                .canManagePetPrivacy(
                        active && manager
                )
                .canUseBasicQr(true)
                .canCustomizeQrColors(
                        active
                                && manager
                                && hasFeature(
                                workspace,
                                PlanFeature.CUSTOM_QR_COLORS
                        )
                )
                .canCustomizeQrStyle(
                        active
                                && manager
                                && hasFeature(
                                workspace,
                                PlanFeature.CUSTOM_QR_STYLE
                        )
                )
                .canUsePetImageInQr(
                        active
                                && manager
                                && hasFeature(
                                workspace,
                                PlanFeature.CUSTOM_QR_LOGO
                        )
                )
                .canPublishHealthSummary(
                        active
                                && manager
                                && hasFeature(
                                workspace,
                                PlanFeature.PUBLIC_HEALTH_SUMMARY
                        )
                )
                .build();
    }

    private WorkspacePermissionsDto noPermissions() {
        return WorkspacePermissionsDto.builder()
                .canAccessWorkspace(false)
                .canViewPreview(false)
                .canViewPets(false)
                .canCreatePets(false)
                .canEditPets(false)
                .canDeletePets(false)
                .canViewHealthRecords(false)
                .canManageHealthRecords(false)
                .canViewMembers(false)
                .canManageMembers(false)
                .canManageBilling(false)
                .canRenameWorkspace(false)
                .canCancelSubscription(false)
                .canReactivateSubscription(false)
                .canManagePetPrivacy(false)
                .canUseBasicQr(false)
                .canCustomizeQrColors(false)
                .canCustomizeQrStyle(false)
                .canUsePetImageInQr(false)
                .canPublishHealthSummary(false)
                .build();
    }
}