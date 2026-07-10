package com.dbp.uripet.pet.service;

import com.dbp.uripet.config.error.InvalidOperationException;
import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.pet.domain.Pet;
import com.dbp.uripet.pet.dto.PetQrSettingsRequestDto;
import com.dbp.uripet.pet.dto.PetQrSettingsResponseDto;
import com.dbp.uripet.pet.repository.PetRepository;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.user.service.UserService;
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.enums.PlanFeature;
import com.dbp.uripet.workspace.service.PlanAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PetQrSettingsService {

    private static final String DEFAULT_FOREGROUND_COLOR =
            "#000000";

    private static final String DEFAULT_BACKGROUND_COLOR =
            "#FFFFFF";

    private final PetRepository petRepository;

    private final UserService userService;

    private final PlanAccessService planAccessService;

    @Transactional(readOnly = true)
    public PetQrSettingsResponseDto getQrSettings(
            String pid
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        planAccessService.checkCanAccessWorkspace(
                pet.getWorkspace(),
                currentUser
        );

        return mapToResponse(pet);
    }

    @Transactional
    public PetQrSettingsResponseDto updateQrSettings(
            String pid,
            PetQrSettingsRequestDto request
    ) {
        Pet pet = findPet(pid);

        Workspace workspace = pet.getWorkspace();

        User currentUser =
                userService.getAuthenticatedUser();

        planAccessService.checkWorkspaceActive(workspace);

        planAccessService.checkCanManageWorkspace(
                workspace,
                currentUser
        );

        /*
         * Colores personalizados.
         */
        if (request.getForegroundColor() != null) {
            planAccessService.checkFeature(
                    workspace,
                    PlanFeature.CUSTOM_QR_COLORS
            );

            pet.setQrForegroundColor(
                    normalizeColor(
                            request.getForegroundColor()
                    )
            );
        }

        if (request.getBackgroundColor() != null) {
            planAccessService.checkFeature(
                    workspace,
                    PlanFeature.CUSTOM_QR_COLORS
            );

            pet.setQrBackgroundColor(
                    normalizeColor(
                            request.getBackgroundColor()
                    )
            );
        }

        /*
         * Estilos personalizados.
         */
        if (request.getPatternStyle() != null) {
            planAccessService.checkFeature(
                    workspace,
                    PlanFeature.CUSTOM_QR_STYLE
            );

            pet.setQrPatternStyle(
                    request.getPatternStyle()
            );
        }

        if (request.getCornerStyle() != null) {
            planAccessService.checkFeature(
                    workspace,
                    PlanFeature.CUSTOM_QR_STYLE
            );

            pet.setQrCornerStyle(
                    request.getCornerStyle()
            );
        }

        if (request.getFrameStyle() != null) {
            planAccessService.checkFeature(
                    workspace,
                    PlanFeature.CUSTOM_QR_STYLE
            );

            pet.setQrFrameStyle(
                    request.getFrameStyle()
            );
        }

        /*
         * Foto de mascota dentro del QR.
         */
        if (request.getShowPetImage() != null) {
            boolean wantsPetImage =
                    request.getShowPetImage();

            if (wantsPetImage) {
                planAccessService.checkFeature(
                        workspace,
                        PlanFeature.CUSTOM_QR_LOGO
                );

                if (pet.getImagesUrl() == null
                        || pet.getImagesUrl().isEmpty()) {

                    throw new InvalidOperationException(
                            "Pet must have an image before enabling it inside the QR code"
                    );
                }
            }

            pet.setQrShowPetImage(wantsPetImage);
        }

        /*
         * Texto inferior.
         *
         * Mostrar una etiqueta básica está disponible
         * para todos los planes.
         */
        if (request.getShowLabel() != null) {
            pet.setQrShowLabel(
                    request.getShowLabel()
            );
        }

        if (request.getLabelText() != null) {
            String labelText =
                    request.getLabelText().trim();

            if (labelText.isBlank()) {
                pet.setQrLabelText(null);
            } else {
                pet.setQrLabelText(labelText);
            }
        }

        if (pet.isQrShowLabel()
                && (pet.getQrLabelText() == null
                || pet.getQrLabelText().isBlank())) {

            pet.setQrLabelText(
                    buildDefaultLabel(pet)
            );
        }

        Pet savedPet = petRepository.save(pet);

        return mapToResponse(savedPet);
    }

    @Transactional
    public PetQrSettingsResponseDto resetQrSettings(
            String pid
    ) {
        Pet pet = findPet(pid);

        Workspace workspace = pet.getWorkspace();

        User currentUser =
                userService.getAuthenticatedUser();

        planAccessService.checkWorkspaceActive(workspace);

        planAccessService.checkCanManageWorkspace(
                workspace,
                currentUser
        );

        pet.setQrForegroundColor(
                DEFAULT_FOREGROUND_COLOR
        );

        pet.setQrBackgroundColor(
                DEFAULT_BACKGROUND_COLOR
        );

        pet.setQrPatternStyle(
                com.dbp.uripet.pet.domain.enums
                        .QrPatternStyle.SQUARE
        );

        pet.setQrCornerStyle(
                com.dbp.uripet.pet.domain.enums
                        .QrCornerStyle.SQUARE
        );

        pet.setQrFrameStyle(
                com.dbp.uripet.pet.domain.enums
                        .QrFrameStyle.NONE
        );

        pet.setQrShowPetImage(false);

        pet.setQrShowLabel(true);

        pet.setQrLabelText(
                buildDefaultLabel(pet)
        );

        Pet savedPet = petRepository.save(pet);

        return mapToResponse(savedPet);
    }

    private Pet findPet(String pid) {
        return petRepository.findByPid(pid)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Pet not found"
                        )
                );
    }

    private String normalizeColor(String color) {
        return color.trim().toUpperCase();
    }

    private String buildDefaultLabel(Pet pet) {
        if (pet.getName() == null
                || pet.getName().isBlank()) {

            return "Mascota encontrada";
        }

        String generated =
                "Encontraste a " + pet.getName();

        if (generated.length() <= 40) {
            return generated;
        }

        return generated.substring(0, 40);
    }

    private PetQrSettingsResponseDto mapToResponse(
            Pet pet
    ) {
        Workspace workspace = pet.getWorkspace();

        boolean colorsAvailable =
                planAccessService.hasFeature(
                        workspace,
                        PlanFeature.CUSTOM_QR_COLORS
                );

        boolean stylesAvailable =
                planAccessService.hasFeature(
                        workspace,
                        PlanFeature.CUSTOM_QR_STYLE
                );

        boolean imageAvailable =
                planAccessService.hasFeature(
                        workspace,
                        PlanFeature.CUSTOM_QR_LOGO
                );

        String petImageUrl = null;

        if (pet.getImagesUrl() != null
                && !pet.getImagesUrl().isEmpty()) {

            petImageUrl = pet.getImagesUrl().get(0);
        }

        return PetQrSettingsResponseDto.builder()
                .petPid(pet.getPid())
                .qrContent(pet.getPid())
                .foregroundColor(
                        pet.getQrForegroundColor()
                )
                .backgroundColor(
                        pet.getQrBackgroundColor()
                )
                .patternStyle(
                        pet.getQrPatternStyle()
                )
                .cornerStyle(
                        pet.getQrCornerStyle()
                )
                .frameStyle(
                        pet.getQrFrameStyle()
                )
                .showPetImage(
                        pet.isQrShowPetImage()
                )
                .showLabel(
                        pet.isQrShowLabel()
                )
                .labelText(
                        pet.getQrLabelText()
                )
                .petImageUrl(petImageUrl)
                .planType(
                        workspace != null
                                ? workspace.getPlanType()
                                : null
                )
                .customColorsAvailable(
                        colorsAvailable
                )
                .customStylesAvailable(
                        stylesAvailable
                )
                .petImageAvailable(
                        imageAvailable
                )
                .build();
    }
}