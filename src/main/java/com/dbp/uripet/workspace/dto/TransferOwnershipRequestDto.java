package com.dbp.uripet.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferOwnershipRequestDto {

    @NotBlank(
            message = "New owner user UID is required"
    )
    private String newOwnerUserUid;
}