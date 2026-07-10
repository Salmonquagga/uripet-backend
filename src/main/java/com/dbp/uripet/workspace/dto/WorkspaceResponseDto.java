package com.dbp.uripet.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceResponseDto {

    private String uid;

    private String name;

    private String ownerUid;

    private String ownerName;

    private String planType;

    private String status;

    private String currentUserRole;

    private boolean currentUserOwner;

    private boolean currentUserCanManage;

    /*
     * Ayuda al frontend a reconocer el espacio personal
     * sin depender únicamente del nombre.
     */
    private boolean personalWorkspace;

    /*
     * Indica si el grupo está completamente operativo.
     */
    private boolean active;

    /*
     * Indica si debe mostrarse como grupo pausado
     * o bloqueado.
     */
    private boolean restricted;

    private long membersCount;

    private long petsCount;

    private WorkspacePermissionsDto permissions;

    private ZonedDateTime createdAt;

    private ZonedDateTime updatedAt;
}