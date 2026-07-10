package com.dbp.uripet.health.service;

import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.health.domain.HealthRecord;
import com.dbp.uripet.health.domain.HealthRecordType;
import com.dbp.uripet.health.dto.HealthRequestDto;
import com.dbp.uripet.health.dto.HealthResponseDto;
import com.dbp.uripet.health.repository.HealthRecordRepository;
import com.dbp.uripet.pet.domain.Pet;
import com.dbp.uripet.pet.repository.PetRepository;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.user.service.UserService;
import com.dbp.uripet.workspace.service.PlanAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HealthService {

    private final HealthRecordRepository
            healthRecordRepository;

    private final PetRepository petRepository;

    private final UserService userService;

    private final PlanAccessService
            planAccessService;

    @Transactional
    public HealthResponseDto createRecord(
            String pid,
            HealthRequestDto request
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        planAccessService
                .checkCanManageHealthRecords(
                        pet.getWorkspace(),
                        currentUser
                );

        HealthRecord record =
                HealthRecord.builder()
                        .pet(pet)
                        .createdBy(currentUser)
                        .type(request.getType())
                        .title(
                                cleanRequiredText(
                                        request.getTitle()
                                )
                        )
                        .description(
                                cleanOptionalText(
                                        request
                                                .getDescription()
                                )
                        )
                        .date(request.getDate())
                        .build();

        return mapToDto(
                healthRecordRepository.save(
                        record
                )
        );
    }

    @Transactional(readOnly = true)
    public Page<HealthResponseDto> getRecords(
            String pid,
            HealthRecordType type,
            int page,
            int size
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        planAccessService
                .checkCanViewHealthRecords(
                        pet.getWorkspace(),
                        currentUser
                );

        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.min(
                                Math.max(size, 1),
                                100
                        ),
                        Sort.by(
                                Sort.Order.desc("date"),
                                Sort.Order.desc("createdAt")
                        )
                );

        Page<HealthRecord> records;

        if (type != null) {
            records = healthRecordRepository
                    .findByPetAndType(
                            pet,
                            type,
                            pageable
                    );
        } else {
            records = healthRecordRepository
                    .findByPet(
                            pet,
                            pageable
                    );
        }

        return records.map(
                this::mapToDto
        );
    }

    @Transactional
    public HealthResponseDto updateRecord(
            String pid,
            Long recordId,
            HealthRequestDto request
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        planAccessService
                .checkCanManageHealthRecords(
                        pet.getWorkspace(),
                        currentUser
                );

        HealthRecord record =
                findRecordForPet(
                        pet,
                        recordId
                );

        if (request.getType() != null) {
            record.setType(
                    request.getType()
            );
        }

        if (request.getTitle() != null) {
            record.setTitle(
                    cleanRequiredText(
                            request.getTitle()
                    )
            );
        }

        if (request.getDescription() != null) {
            record.setDescription(
                    cleanOptionalText(
                            request.getDescription()
                    )
            );
        }

        if (request.getDate() != null) {
            record.setDate(
                    request.getDate()
            );
        }

        return mapToDto(
                healthRecordRepository.save(
                        record
                )
        );
    }

    @Transactional
    public void deleteRecord(
            String pid,
            Long recordId
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        planAccessService
                .checkCanManageHealthRecords(
                        pet.getWorkspace(),
                        currentUser
                );

        HealthRecord record =
                findRecordForPet(
                        pet,
                        recordId
                );

        healthRecordRepository.delete(record);
    }

    private Pet findPet(
            String pid
    ) {
        if (!StringUtils.hasText(pid)) {
            throw new ResourceNotFoundException(
                    "Pet PID is required"
            );
        }

        return petRepository
                .findByPid(pid.trim())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Pet not found"
                        )
                );
    }

    private HealthRecord findRecordForPet(
            Pet pet,
            Long recordId
    ) {
        if (recordId == null) {
            throw new ResourceNotFoundException(
                    "Health record ID is required"
            );
        }

        HealthRecord record =
                healthRecordRepository
                        .findById(recordId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Health record not found"
                                )
                        );

        if (!record.getPet()
                .getId()
                .equals(pet.getId())) {

            /*
             * Se responde como no encontrado
             * para no revelar registros de otra mascota.
             */
            throw new ResourceNotFoundException(
                    "Health record not found for this pet"
            );
        }

        return record;
    }

    private String cleanRequiredText(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        return value.trim();
    }

    private String cleanOptionalText(
            String value
    ) {
        if (value == null) {
            return null;
        }

        String cleaned = value.trim();

        return cleaned.isBlank()
                ? null
                : cleaned;
    }

    private HealthResponseDto mapToDto(
            HealthRecord record
    ) {
        return HealthResponseDto.builder()
                .id(record.getId())
                .createdByUid(
                        record
                                .getCreatedBy()
                                .getUid()
                )
                .type(record.getType())
                .title(record.getTitle())
                .description(
                        record.getDescription()
                )
                .date(record.getDate())
                .createdAt(
                        record.getCreatedAt()
                )
                .build();
    }
}