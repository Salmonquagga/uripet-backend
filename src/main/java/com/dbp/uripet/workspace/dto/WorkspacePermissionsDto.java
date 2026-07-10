package com.dbp.uripet.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspacePermissionsDto {

    /*
     * Permisos generales.
     */
    private boolean canAccessWorkspace;

    private boolean canViewPreview;

    /*
     * Mascotas.
     */
    private boolean canViewPets;

    private boolean canCreatePets;

    private boolean canEditPets;

    private boolean canDeletePets;

    /*
     * Historial médico.
     */
    private boolean canViewHealthRecords;

    private boolean canManageHealthRecords;

    /*
     * Miembros.
     */
    private boolean canViewMembers;

    private boolean canManageMembers;

    /*
     * Facturación.
     *
     * Solo el OWNER debe administrar pagos,
     * suscripción, cancelación y reactivación.
     */
    private boolean canManageBilling;

    /*
     * Configuración del workspace.
     */
    private boolean canRenameWorkspace;

    private boolean canCancelSubscription;

    private boolean canReactivateSubscription;

    /*
     * Configuración pública y QR.
     */
    private boolean canManagePetPrivacy;

    private boolean canUseBasicQr;

    private boolean canCustomizeQrColors;

    private boolean canCustomizeQrStyle;

    private boolean canUsePetImageInQr;

    private boolean canPublishHealthSummary;
}