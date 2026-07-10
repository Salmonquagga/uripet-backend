package com.dbp.uripet.pet.controller;

import com.dbp.uripet.pet.dto.PetPrivacySettingsRequestDto;
import com.dbp.uripet.pet.dto.PetPrivacySettingsResponseDto;
import com.dbp.uripet.pet.dto.PetQrDataDto;
import com.dbp.uripet.pet.dto.PetQrSettingsRequestDto;
import com.dbp.uripet.pet.dto.PetQrSettingsResponseDto;
import com.dbp.uripet.pet.dto.PetRequestDto;
import com.dbp.uripet.pet.dto.PetResponseDto;
import com.dbp.uripet.pet.dto.PetResponsibleRequestDto;
import com.dbp.uripet.pet.dto.PetResponsibleResponseDto;
import com.dbp.uripet.pet.dto.PublicPetResponseDto;
import com.dbp.uripet.pet.service.PetPrivacyService;
import com.dbp.uripet.pet.service.PetQrSettingsService;
import com.dbp.uripet.pet.service.PetService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PetController {

    private final PetService petService;

    private final PetPrivacyService petPrivacyService;

    private final PetQrSettingsService petQrSettingsService;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PetResponseDto> createPet(
            @Valid @RequestBody PetRequestDto request
    ) {
        return new ResponseEntity<>(
                petService.createPet(request),
                HttpStatus.CREATED
        );
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PetResponseDto> createPetForm(
            @Valid @ModelAttribute PetRequestDto request
    ) {
        return new ResponseEntity<>(
                petService.createPet(request),
                HttpStatus.CREATED
        );
    }

    /*
     * Perfil público consultado al escanear el QR.
     */
    @GetMapping("/public/{pid}")
    public ResponseEntity<PublicPetResponseDto> getPublicPet(
            @PathVariable String pid
    ) {
        return ResponseEntity.ok(
                petPrivacyService.getPublicPet(pid)
        );
    }

    @GetMapping("/{pid}")
    public ResponseEntity<PetResponseDto> getPet(
            @PathVariable String pid
    ) {
        return ResponseEntity.ok(
                petService.getPet(pid)
        );
    }

    @PatchMapping(
            value = "/{pid}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<PetResponseDto> updatePet(
            @PathVariable String pid,
            @Valid @RequestBody PetRequestDto request
    ) {
        return ResponseEntity.ok(
                petService.updatePet(pid, request)
        );
    }

    @PatchMapping(
            value = "/{pid}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PetResponseDto> updatePetForm(
            @PathVariable String pid,
            @Valid @ModelAttribute PetRequestDto request
    ) {
        return ResponseEntity.ok(
                petService.updatePet(pid, request)
        );
    }

    @DeleteMapping("/{pid}")
    public ResponseEntity<Void> deletePet(
            @PathVariable String pid
    ) {
        petService.deletePet(pid);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{pid}/images")
    public ResponseEntity<PetResponseDto> removePetImage(
            @PathVariable String pid,
            @RequestParam String imageUrl
    ) {
        String decodedImageUrl = URLDecoder.decode(
                imageUrl,
                StandardCharsets.UTF_8
        );

        return ResponseEntity.ok(
                petService.removeImage(
                        pid,
                        decodedImageUrl
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<Page<PetResponseDto>> getMyPets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false)
            String workspaceUid
    ) {
        return ResponseEntity.ok(
                petService.getMyPets(
                        page,
                        size,
                        search,
                        workspaceUid
                )
        );
    }

    @GetMapping("/user/{uid}")
    public ResponseEntity<List<PetResponseDto>>
    getPetsByUser(
            @PathVariable String uid
    ) {
        return ResponseEntity.ok(
                petService.getPetsByUser(uid)
        );
    }

    /*
     * Información mínima utilizada para crear el QR.
     */
    @GetMapping("/{pid}/qr-data")
    public ResponseEntity<PetQrDataDto> getQrData(
            @PathVariable String pid
    ) {
        return ResponseEntity.ok(
                petService.getQrData(pid)
        );
    }

    /*
     * Consultar configuración visual del QR.
     */
    @GetMapping("/{pid}/qr-settings")
    public ResponseEntity<PetQrSettingsResponseDto>
    getQrSettings(
            @PathVariable String pid
    ) {
        return ResponseEntity.ok(
                petQrSettingsService.getQrSettings(pid)
        );
    }

    /*
     * Modificar configuración visual del QR.
     */
    @PatchMapping("/{pid}/qr-settings")
    public ResponseEntity<PetQrSettingsResponseDto>
    updateQrSettings(
            @PathVariable String pid,
            @Valid
            @RequestBody
            PetQrSettingsRequestDto request
    ) {
        return ResponseEntity.ok(
                petQrSettingsService.updateQrSettings(
                        pid,
                        request
                )
        );
    }

    /*
     * Restaurar configuración original del QR.
     */
    @PostMapping("/{pid}/qr-settings/reset")
    public ResponseEntity<PetQrSettingsResponseDto>
    resetQrSettings(
            @PathVariable String pid
    ) {
        return ResponseEntity.ok(
                petQrSettingsService.resetQrSettings(pid)
        );
    }

    /*
     * Consultar privacidad del perfil público.
     */
    @GetMapping("/{pid}/privacy-settings")
    public ResponseEntity<PetPrivacySettingsResponseDto>
    getPrivacySettings(
            @PathVariable String pid
    ) {
        return ResponseEntity.ok(
                petPrivacyService.getPrivacySettings(pid)
        );
    }

    /*
     * Modificar privacidad del perfil público.
     */
    @PatchMapping("/{pid}/privacy-settings")
    public ResponseEntity<PetPrivacySettingsResponseDto>
    updatePrivacySettings(
            @PathVariable String pid,
            @Valid
            @RequestBody
            PetPrivacySettingsRequestDto request
    ) {
        return ResponseEntity.ok(
                petPrivacyService.updatePrivacySettings(
                        pid,
                        request
                )
        );
    }

    @PostMapping("/{pid}/responsibles")
    public ResponseEntity<PetResponsibleResponseDto>
    addResponsible(
            @PathVariable String pid,
            @Valid
            @RequestBody
            PetResponsibleRequestDto request
    ) {
        return new ResponseEntity<>(
                petService.addResponsible(pid, request),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/{pid}/responsibles")
    public ResponseEntity<List<PetResponsibleResponseDto>>
    getResponsibles(
            @PathVariable String pid
    ) {
        return ResponseEntity.ok(
                petService.getResponsibles(pid)
        );
    }

    @DeleteMapping(
            "/{pid}/responsibles/{userUid}"
    )
    public ResponseEntity<Void> removeResponsible(
            @PathVariable String pid,
            @PathVariable String userUid
    ) {
        petService.removeResponsible(
                pid,
                userUid
        );

        return ResponseEntity.noContent().build();
    }
}