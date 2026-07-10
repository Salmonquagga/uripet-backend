package com.dbp.uripet.storage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadRequestDto {
    @NotBlank(message = "Pet PID is required")
    private String petPid;

    @NotNull(message = "Image file is required")
    private MultipartFile image;

    @NotNull(message = "Image index is required")
    @Min(value = 1, message = "Image index must be greater than or equal to 1")
    @Max(value = 3, message = "Image index must be between 1 and 3")
    private Integer imageIndex;
}