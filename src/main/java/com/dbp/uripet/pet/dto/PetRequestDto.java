package com.dbp.uripet.pet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Data
public class PetRequestDto {

    // Grupo donde se creará la mascota.
    // Si llega vacío o null, se usará el espacio personal gratis del usuario.
    private String workspaceUid;

    @NotBlank(message = "Pet name is required")
    @Size(min = 2, max = 60, message = "Pet name must be between 2 and 60 characters")
    private String name;

    @NotBlank(message = "Species is required")
    @Size(max = 40, message = "Species must not exceed 40 characters")
    private String species;

    @Size(max = 60, message = "Breed must not exceed 60 characters")
    private String breed;

    @PastOrPresent(message = "Birth date cannot be in the future")
    private LocalDate birthDate;

    @DecimalMin(value = "0.1", message = "Weight must be greater than 0")
    private Double weight;

    @Size(max = 40, message = "Color must not exceed 40 characters")
    private String color;

    @Size(max = 120, message = "Emergency contact must not exceed 120 characters")
    private String emergencyContact;

    @Size(max = 3, message = "A pet can have at most 3 image URLs")
    private List<@NotBlank(message = "Image URL cannot be blank") String> imagesUrl;

    @Size(max = 3, message = "A pet can have at most 3 images")
    private List<@NotNull(message = "Image file cannot be null") MultipartFile> images;
}
