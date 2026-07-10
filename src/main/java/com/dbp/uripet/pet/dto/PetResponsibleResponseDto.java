package com.dbp.uripet.pet.dto;
import com.dbp.uripet.pet.domain.enums.AccessLevel;
import com.dbp.uripet.pet.domain.enums.ResponsibleRole;
import lombok.Builder; import lombok.Data;
import java.time.ZonedDateTime;
@Data @Builder public class PetResponsibleResponseDto {
    private String userUid;
    private String userName;
    private String userEmail;
    private AccessLevel accessLevel;
    private ResponsibleRole responsibleRole;
    private ZonedDateTime createdAt;
}
