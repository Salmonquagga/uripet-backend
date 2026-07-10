package com.dbp.uripet.pet.service;

import com.dbp.uripet.config.error.ForbiddenException;
import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.health.domain.HealthRecord;
import com.dbp.uripet.health.repository.HealthRecordRepository;
import com.dbp.uripet.pet.domain.Pet;
import com.dbp.uripet.pet.dto.PetPrivacySettingsRequestDto;
import com.dbp.uripet.pet.dto.PetPrivacySettingsResponseDto;
import com.dbp.uripet.pet.dto.PublicPetHealthSummaryDto;
import com.dbp.uripet.pet.dto.PublicPetResponseDto;
import com.dbp.uripet.pet.repository.PetRepository;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.user.service.UserService;
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.enums.PlanFeature;
import com.dbp.uripet.workspace.domain.enums.WorkspaceStatus;
import com.dbp.uripet.workspace.service.PlanAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PetPrivacyService {

    private final PetRepository petRepository;

    private final HealthRecordRepository healthRecordRepository;

    private final UserService userService;

    private final PlanAccessService planAccessService;

    @Transactional(readOnly = true)
    public PetPrivacySettingsResponseDto getPrivacySettings(
            String pid
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        planAccessService.checkCanAccessWorkspace(
                pet.getWorkspace(),
                currentUser
        );

        return mapPrivacySettings(pet);
    }

    @Transactional
    public PetPrivacySettingsResponseDto updatePrivacySettings(
            String pid,
            PetPrivacySettingsRequestDto request
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

        if (request.getShowEmergencyContact() != null) {
            pet.setShowEmergencyContact(
                    request.getShowEmergencyContact()
            );
        }

        if (request.getShowBasicInformation() != null) {
            pet.setShowBasicInformation(
                    request.getShowBasicInformation()
            );
        }

        if (request.getShowHealthSummary() != null) {
            boolean wantsHealthSummary =
                    request.getShowHealthSummary();

            if (wantsHealthSummary) {
                planAccessService.checkFeature(
                        workspace,
                        PlanFeature.PUBLIC_HEALTH_SUMMARY
                );
            }

            pet.setShowHealthSummary(wantsHealthSummary);
        }

        Pet savedPet = petRepository.save(pet);

        return mapPrivacySettings(savedPet);
    }

    @Transactional(readOnly = true)
    public PublicPetResponseDto getPublicPet(
            String pid
    ) {
        Pet pet = findPet(pid);
        Workspace workspace = pet.getWorkspace();

        String mainImageUrl = null;

        if (pet.getImagesUrl() != null
                && !pet.getImagesUrl().isEmpty()) {

            mainImageUrl = pet.getImagesUrl().get(0);
        }

        PublicPetResponseDto.PublicPetResponseDtoBuilder response =
                PublicPetResponseDto.builder()
                        .pid(pet.getPid())
                        .name(pet.getName())
                        .mainImageUrl(mainImageUrl);

        if (pet.isShowBasicInformation()) {
            response
                    .species(pet.getSpecies())
                    .breed(pet.getBreed())
                    .birthDate(pet.getBirthDate())
                    .color(pet.getColor());
        }

        if (pet.isShowEmergencyContact()) {
            response.emergencyContact(
                    pet.getEmergencyContact()
            );
        }

        boolean mayShowHealthSummary =
                pet.isShowHealthSummary()
                        && workspace != null
                        && workspace.getStatus()
                        == WorkspaceStatus.ACTIVE
                        && planAccessService.hasFeature(
                        workspace,
                        PlanFeature.PUBLIC_HEALTH_SUMMARY
                );

        if (mayShowHealthSummary) {
            response.healthSummary(
                    buildPublicHealthSummary(pet)
            );
        }

        return response.build();
    }

    private Pet findPet(String pid) {
        return petRepository.findByPid(pid)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Pet not found"
                        )
                );
    }

    private PetPrivacySettingsResponseDto mapPrivacySettings(
            Pet pet
    ) {
        Workspace workspace = pet.getWorkspace();

        boolean healthSummaryAvailable =
                workspace != null
                        && planAccessService.hasFeature(
                        workspace,
                        PlanFeature.PUBLIC_HEALTH_SUMMARY
                );

        return PetPrivacySettingsResponseDto.builder()
                .petPid(pet.getPid())
                .showEmergencyContact(
                        pet.isShowEmergencyContact()
                )
                .showBasicInformation(
                        pet.isShowBasicInformation()
                )
                .showHealthSummary(
                        pet.isShowHealthSummary()
                )
                .healthSummaryAvailableForPlan(
                        healthSummaryAvailable
                )
                .planType(
                        workspace != null
                                ? workspace.getPlanType()
                                : null
                )
                .build();
    }

    private List<PublicPetHealthSummaryDto>
    buildPublicHealthSummary(Pet pet) {

        return healthRecordRepository.findByPet(pet)
                .stream()
                .sorted(
                        Comparator
                                .comparing(
                                        this::resolveRecordDate,
                                        Comparator.nullsLast(
                                                Comparator.reverseOrder()
                                        )
                                )
                )
                .limit(5)
                .map(record ->
                        PublicPetHealthSummaryDto.builder()
                                .type(record.getType())
                                .title(record.getTitle())
                                .date(record.getDate())
                                .build()
                )
                .toList();
    }

    private ZonedDateTime resolveRecordDate(
            HealthRecord record
    ) {
        if (record.getDate() != null) {
            LocalDate date = record.getDate();

            return date
                    .atStartOfDay(
                            java.time.ZoneId.systemDefault()
                    );
        }

        return record.getCreatedAt();
    }
}