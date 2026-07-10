package com.dbp.uripet.health.dto;
import lombok.Builder; import lombok.Data;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import com.dbp.uripet.health.domain.HealthRecordType;
@Data @Builder public class HealthResponseDto {
    private Long id;
    private String createdByUid;
    private HealthRecordType type;
    private String title;
    private String description;
    private LocalDate date;
    private ZonedDateTime createdAt;
}
