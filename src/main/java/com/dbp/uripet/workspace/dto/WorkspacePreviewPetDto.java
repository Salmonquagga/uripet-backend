package com.dbp.uripet.workspace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspacePreviewPetDto {

    private String pid;

    private String name;

    private String mainImageUrl;
}