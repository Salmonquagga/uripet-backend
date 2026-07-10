package com.dbp.uripet.pet.dto;

import com.dbp.uripet.workspace.domain.enums.PlanType;
import com.dbp.uripet.workspace.domain.enums.WorkspaceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PetResponseDto {
    private String pid;
    private String name;
    private String species;
    private String breed;
    private LocalDate birthDate;
    private Double weight;
    private String color;
    private UUID qrCode;
    private String emergencyContact;
    private List<String> imagesUrl;
    private ZonedDateTime createdAt;

    private String workspaceUid;
    private String workspaceName;
    private PlanType planType;
    private WorkspaceStatus workspaceStatus;
}
