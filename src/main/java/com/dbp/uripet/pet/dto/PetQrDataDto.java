package com.dbp.uripet.pet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetQrDataDto {
    private String pid;

    @JsonProperty("public")
    private boolean isPublic;
}