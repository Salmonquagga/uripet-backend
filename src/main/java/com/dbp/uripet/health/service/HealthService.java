package com.dbp.uripet.health.service;

import com.dbp.uripet.config.error.ForbiddenException;
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
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.WorkspaceMember;
import com.dbp.uripet.workspace.domain.enums.WorkspaceRole;
import com.dbp.uripet.workspace.domain.enums.WorkspaceStatus;
import com.dbp.uripet.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HealthService {
    private final HealthRecordRepository healthRecordRepository;
    private final PetRepository petRepository;
    private final UserService userService;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional
    public HealthResponseDto createRecord(String pid, HealthRequestDto request) {
        Pet pet = petRepository.findByPid(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        User user = userService.getAuthenticatedUser();
        checkWorkspaceActive(pet.getWorkspace());
        checkCanManageWorkspace(pet.getWorkspace(), user);

        HealthRecord record = HealthRecord.builder()
                .pet(pet)
                .createdBy(user)
                .type(request.getType())
                .title(request.getTitle())
                .description(request.getDescription())
                .date(request.getDate())
                .build();

        healthRecordRepository.save(record);

        return mapToDto(record);
    }

    @Transactional(readOnly = true)
    public Page<HealthResponseDto> getRecords(
            String pid,
            HealthRecordType type,
            int page,
            int size
    ) {
        Pet pet = petRepository.findByPid(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        User user = userService.getAuthenticatedUser();
        checkWorkspaceActive(pet.getWorkspace());
        checkCanAccessWorkspace(pet.getWorkspace(), user);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "date")
        );

        Page<HealthRecord> records;

        if (type != null) {
            records = healthRecordRepository.findByPetAndType(pet, type, pageable);
        } else {
            records = healthRecordRepository.findByPet(pet, pageable);
        }

        return records.map(this::mapToDto);
    }

    @Transactional
    public HealthResponseDto updateRecord(
            String pid,
            Long recordId,
            HealthRequestDto request
    ) {
        Pet pet = petRepository.findByPid(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        User user = userService.getAuthenticatedUser();
        checkWorkspaceActive(pet.getWorkspace());
        checkCanManageWorkspace(pet.getWorkspace(), user);

        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        if (!record.getPet().getId().equals(pet.getId())) {
            throw new ResourceNotFoundException("Record does not belong to this pet");
        }

        if (request.getType() != null) {
            record.setType(request.getType());
        }

        if (request.getTitle() != null) {
            record.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            record.setDescription(request.getDescription());
        }

        if (request.getDate() != null) {
            record.setDate(request.getDate());
        }

        healthRecordRepository.save(record);

        return mapToDto(record);
    }

    @Transactional
    public void deleteRecord(String pid, Long recordId) {
        Pet pet = petRepository.findByPid(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        User user = userService.getAuthenticatedUser();
        checkWorkspaceActive(pet.getWorkspace());
        checkCanManageWorkspace(pet.getWorkspace(), user);

        HealthRecord record = healthRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        if (!record.getPet().getId().equals(pet.getId())) {
            throw new ResourceNotFoundException("Record does not belong to this pet");
        }

        healthRecordRepository.delete(record);
    }

    private WorkspaceMember checkCanAccessWorkspace(Workspace workspace, User user) {
        return workspaceMemberRepository.findByWorkspaceAndUser(workspace, user)
                .orElseThrow(() -> new ForbiddenException("Not authorized to access this workspace"));
    }

    private WorkspaceMember checkCanManageWorkspace(Workspace workspace, User user) {
        WorkspaceMember member = checkCanAccessWorkspace(workspace, user);

        if (member.getRole() != WorkspaceRole.OWNER && member.getRole() != WorkspaceRole.ADMIN) {
            throw new ForbiddenException("Owner or admin role required");
        }

        return member;
    }

    private void checkWorkspaceActive(Workspace workspace) {
        if (workspace.getStatus() != WorkspaceStatus.ACTIVE) {
            throw new ForbiddenException("Workspace is not active");
        }
    }

    private HealthResponseDto mapToDto(HealthRecord record) {
        return HealthResponseDto.builder()
                .id(record.getId())
                .createdByUid(record.getCreatedBy().getUid())
                .type(record.getType())
                .title(record.getTitle())
                .description(record.getDescription())
                .date(record.getDate())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
