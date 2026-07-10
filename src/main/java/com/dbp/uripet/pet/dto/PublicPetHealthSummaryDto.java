package com.dbp.uripet.pet.dto;

import com.dbp.uripet.health.domain.HealthRecordType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicPetHealthSummaryDto {

    private HealthRecordType type;

    private String title;

    private LocalDate date;
}