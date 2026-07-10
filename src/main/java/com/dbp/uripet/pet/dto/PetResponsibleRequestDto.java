package com.dbp.uripet.pet.dto;
import com.dbp.uripet.pet.domain.enums.AccessLevel;
import com.dbp.uripet.pet.domain.enums.ResponsibleRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data public class PetResponsibleRequestDto {
    @NotBlank(message = "User UID is required")
    private String userUid;

    @NotNull(message = "Access level is required")
    private AccessLevel accessLevel;

    @NotNull(message = "Responsible role is required")
    private ResponsibleRole responsibleRole;
}
