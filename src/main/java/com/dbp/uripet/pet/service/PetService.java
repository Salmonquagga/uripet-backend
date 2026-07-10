package com.dbp.uripet.pet.service;

import com.dbp.uripet.config.error.DuplicateResourceException;
import com.dbp.uripet.config.error.ForbiddenException;
import com.dbp.uripet.config.error.InvalidOperationException;
import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.events.UserAddedToPetEvent;
import com.dbp.uripet.pet.domain.Pet;
import com.dbp.uripet.pet.domain.PetResponsible;
import com.dbp.uripet.pet.domain.enums.AccessLevel;
import com.dbp.uripet.pet.domain.enums.ResponsibleRole;
import com.dbp.uripet.pet.dto.PetQrDataDto;
import com.dbp.uripet.pet.dto.PetRequestDto;
import com.dbp.uripet.pet.dto.PetResponseDto;
import com.dbp.uripet.pet.dto.PetResponsibleRequestDto;
import com.dbp.uripet.pet.dto.PetResponsibleResponseDto;
import com.dbp.uripet.pet.repository.PetRepository;
import com.dbp.uripet.pet.repository.PetResponsibleRepository;
import com.dbp.uripet.storage.service.ImageStorageService;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.user.repository.UserRepository;
import com.dbp.uripet.user.service.UserService;
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.WorkspaceMember;
import com.dbp.uripet.workspace.domain.enums.PlanType;
import com.dbp.uripet.workspace.domain.enums.WorkspaceRole;
import com.dbp.uripet.workspace.domain.enums.WorkspaceStatus;
import com.dbp.uripet.workspace.repository.WorkspaceMemberRepository;
import com.dbp.uripet.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final PetResponsibleRepository petResponsibleRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ImageStorageService imageStorageService;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Transactional
    public PetResponseDto createPet(PetRequestDto request) {
        User user = userService.getAuthenticatedUser();
        Workspace workspace = resolveWorkspaceForCreate(request.getWorkspaceUid(), user);

        checkWorkspaceActive(workspace);
        checkCanManageWorkspace(workspace, user);
        checkPetLimit(workspace);

        Pet pet = Pet.builder()
                .workspace(workspace)
                .name(request.getName())
                .species(request.getSpecies())
                .breed(request.getBreed())
                .birthDate(request.getBirthDate())
                .weight(request.getWeight())
                .color(request.getColor())
                .emergencyContact(request.getEmergencyContact())
                .build();

        petRepository.save(pet);

        List<String> imagesUrl = resolveImages(pet, request, null);
        pet.setImagesUrl(imagesUrl);
        petRepository.save(pet);

        // Compatibilidad con la lógica antigua de responsables por mascota.
        if (!petResponsibleRepository.existsByPetAndUser(pet, user)) {
            PetResponsible responsible = PetResponsible.builder()
                    .pet(pet)
                    .user(user)
                    .accessLevel(AccessLevel.EDITOR)
                    .responsibleRole(ResponsibleRole.OWNER)
                    .build();

            petResponsibleRepository.save(responsible);
        }

        return mapToDto(pet);
    }

    @Transactional(readOnly = true)
    public PetResponseDto getPet(String pid) {
        Pet pet = petRepository.findByPid(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        User user = userService.getAuthenticatedUser();
        checkCanAccessWorkspace(pet.getWorkspace(), user);

        return mapToDto(pet);
    }

    @Transactional(readOnly = true)
    public PetResponseDto getPublicPet(String pid) {
        Pet pet = petRepository.findByPid(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        return mapToDto(pet);
    }

    @Transactional
    public PetResponseDto updatePet(String pid, PetRequestDto request) {
        Pet pet = petRepository.findByPid(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        User user = userService.getAuthenticatedUser();
        checkWorkspaceActive(pet.getWorkspace());
        checkCanManageWorkspace(pet.getWorkspace(), user);

        if (request.getName() != null) pet.setName(request.getName());
        if (request.getSpecies() != null) pet.setSpecies(request.getSpecies());
        if (request.getBreed() != null) pet.setBreed(request.getBreed());
        if (request.getBirthDate() != null) pet.setBirthDate(request.getBirthDate());
        if (request.getWeight() != null) pet.setWeight(request.getWeight());
        if (request.getColor() != null) pet.setColor(request.getColor());
        if (request.getEmergencyContact() != null) pet.setEmergencyContact(request.getEmergencyContact());

        List<String> imagesUrl = resolveImages(pet, request, pet.getImagesUrl());
        pet.setImagesUrl(imagesUrl);

        petRepository.save(pet);
        return mapToDto(pet);
    }

    @Transactional
    public void deletePet(String pid) {
        Pet pet = petRepository.findByPid(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        User user = userService.getAuthenticatedUser();
        checkWorkspaceActive(pet.getWorkspace());
        checkCanManageWorkspace(pet.getWorkspace(), user);

        petResponsibleRepository.deleteAll(petResponsibleRepository.findByPet(pet));
        petRepository.delete(pet);
    }

    @Transactional(readOnly = true)
    public Page<PetResponseDto> getMyPets(int page, int size, String search, String workspaceUid) {
        User user = userService.getAuthenticatedUser();

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Pet> pets;
        String cleanSearch = search == null ? null : search.trim();

        if (workspaceUid != null && !workspaceUid.isBlank()) {
            Workspace workspace = workspaceRepository.findByUid(workspaceUid)
                    .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

            checkCanAccessWorkspace(workspace, user);

            if (cleanSearch != null && !cleanSearch.isEmpty()) {
                pets = petRepository.findByWorkspaceAndNameContainingIgnoreCase(workspace, cleanSearch, pageable);
            } else {
                pets = petRepository.findByWorkspace(workspace, pageable);
            }
        } else {
            List<Workspace> workspaces = getAccessibleWorkspaces(user);

            if (workspaces.isEmpty()) {
                return Page.empty(pageable);
            }

            if (cleanSearch != null && !cleanSearch.isEmpty()) {
                pets = petRepository.findByWorkspaceInAndNameContainingIgnoreCase(workspaces, cleanSearch, pageable);
            } else {
                pets = petRepository.findByWorkspaceIn(workspaces, pageable);
            }
        }

        return pets.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<PetResponseDto> getPetsByUser(String uid) {
        User user = userRepository.findByUid(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Workspace> workspaces = getAccessibleWorkspaces(user);

        if (workspaces.isEmpty()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "createdAt"));

        return petRepository.findByWorkspaceIn(workspaces, pageable)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PetQrDataDto getQrData(String pid) {
        Pet pet = petRepository.findByPid(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        return PetQrDataDto.builder()
                .pid(pet.getPid())
                .isPublic(true)
                .build();
    }

    @Transactional
    public PetResponsibleResponseDto addResponsible(String pid, PetResponsibleRequestDto request) {
        Pet pet = petRepository.findByPid(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        User currentUser = userService.getAuthenticatedUser();
        Workspace workspace = pet.getWorkspace();

        checkWorkspaceActive(workspace);
        checkCanManageWorkspace(workspace, currentUser);

        User targetUser = userRepository.findByUid(request.getUserUid())
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        if (!workspaceMemberRepository.existsByWorkspaceAndUser(workspace, targetUser)) {
            WorkspaceMember workspaceMember = WorkspaceMember.builder()
                    .workspace(workspace)
                    .user(targetUser)
                    .role(mapToWorkspaceRole(request))
                    .build();

            workspaceMemberRepository.save(workspaceMember);
        }

        if (petResponsibleRepository.existsByPetAndUser(pet, targetUser)) {
            throw new DuplicateResourceException("User is already responsible for this pet");
        }

        PetResponsible newResp = PetResponsible.builder()
                .pet(pet)
                .user(targetUser)
                .accessLevel(request.getAccessLevel())
                .responsibleRole(request.getResponsibleRole())
                .build();

        petResponsibleRepository.save(newResp);

        eventPublisher.publishEvent(new UserAddedToPetEvent(targetUser.getEmail(), pet.getName()));

        return mapToRespDto(newResp);
    }

    @Transactional(readOnly = true)
    public List<PetResponsibleResponseDto> getResponsibles(String pid) {
        Pet pet = petRepository.findByPid(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        User user = userService.getAuthenticatedUser();
        checkCanAccessWorkspace(pet.getWorkspace(), user);

        return petResponsibleRepository.findByPet(pet)
                .stream()
                .map(this::mapToRespDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeResponsible(String pid, String userUid) {
        Pet pet = petRepository.findByPid(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        User currentUser = userService.getAuthenticatedUser();
        Workspace workspace = pet.getWorkspace();

        checkWorkspaceActive(workspace);
        checkCanManageWorkspace(workspace, currentUser);

        User targetUser = userRepository.findByUid(userUid)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found"));

        PetResponsible targetResp = petResponsibleRepository.findByPetAndUser(pet, targetUser)
                .orElseThrow(() -> new ResourceNotFoundException("Responsible not found"));

        if (targetResp.getResponsibleRole() == ResponsibleRole.OWNER) {
            int ownerCount = petResponsibleRepository.countByPetAndResponsibleRole(
                    pet,
                    ResponsibleRole.OWNER
            );

            if (ownerCount <= 1) {
                throw new InvalidOperationException("Cannot remove the last owner");
            }
        }

        petResponsibleRepository.delete(targetResp);

        workspaceMemberRepository.findByWorkspaceAndUser(workspace, targetUser)
                .ifPresent(member -> {
                    if (member.getRole() == WorkspaceRole.OWNER) {
                        throw new InvalidOperationException("Cannot remove the workspace owner from this endpoint");
                    }
                    workspaceMemberRepository.delete(member);
                });
    }

    @Transactional
    public PetResponseDto removeImage(String pid, String imageUrl) {
        Pet pet = petRepository.findByPid(pid)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        User user = userService.getAuthenticatedUser();
        checkWorkspaceActive(pet.getWorkspace());
        checkCanManageWorkspace(pet.getWorkspace(), user);

        List<String> currentImages = pet.getImagesUrl() == null
                ? new ArrayList<>()
                : new ArrayList<>(pet.getImagesUrl());

        boolean removed = currentImages.removeIf(image -> image.equals(imageUrl));

        if (!removed) {
            throw new ResourceNotFoundException("Image not found");
        }

        pet.setImagesUrl(currentImages);
        petRepository.save(pet);

        return mapToDto(pet);
    }

    private Workspace resolveWorkspaceForCreate(String workspaceUid, User user) {
        if (workspaceUid != null && !workspaceUid.isBlank()) {
            Workspace workspace = workspaceRepository.findByUid(workspaceUid)
                    .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

            checkCanAccessWorkspace(workspace, user);
            return workspace;
        }

        return getPersonalWorkspace(user);
    }

    private Workspace getPersonalWorkspace(User user) {
        return workspaceMemberRepository.findByUser(user)
                .stream()
                .map(WorkspaceMember::getWorkspace)
                .filter(workspace -> workspace.getPlanType() == PlanType.FREE)
                .filter(workspace -> workspace.getOwner() != null && workspace.getOwner().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Personal workspace not found"));
    }

    private List<Workspace> getAccessibleWorkspaces(User user) {
        return workspaceMemberRepository.findByUser(user)
                .stream()
                .map(WorkspaceMember::getWorkspace)
                .distinct()
                .collect(Collectors.toList());
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

    private void checkPetLimit(Workspace workspace) {
        if (workspace.getPlanType() == PlanType.FREE && petRepository.countByWorkspace(workspace) >= 1) {
            throw new InvalidOperationException("Free workspace can only have one pet");
        }
    }

    private WorkspaceRole mapToWorkspaceRole(PetResponsibleRequestDto request) {
        if (request.getResponsibleRole() == ResponsibleRole.OWNER) {
            return WorkspaceRole.ADMIN;
        }
        if (request.getAccessLevel() == AccessLevel.EDITOR) {
            return WorkspaceRole.ADMIN;
        }
        return WorkspaceRole.MEMBER;
    }

    private List<String> resolveImages(
            Pet pet,
            PetRequestDto request,
            List<String> existingImages
    ) {
        List<String> finalImages = new ArrayList<>();

        if (request.getImagesUrl() != null) {
            for (String imageUrl : request.getImagesUrl()) {
                if (imageUrl != null && !imageUrl.isBlank() && !finalImages.contains(imageUrl)) {
                    finalImages.add(imageUrl);
                }
            }
        } else if (existingImages != null) {
            for (String imageUrl : existingImages) {
                if (imageUrl != null && !imageUrl.isBlank() && !finalImages.contains(imageUrl)) {
                    finalImages.add(imageUrl);
                }
            }
        }

        if (request.getImages() != null && !request.getImages().isEmpty()) {
            List<String> uploadedImages = imageStorageService.storePetImages(
                    pet.getPid(),
                    request.getImages()
            );

            for (String uploadedImage : uploadedImages) {
                if (uploadedImage != null && !uploadedImage.isBlank() && !finalImages.contains(uploadedImage)) {
                    finalImages.add(uploadedImage);
                }
            }
        }

        if (finalImages.size() > 3) {
            throw new InvalidOperationException("A pet can have at most 3 images");
        }

        return finalImages;
    }

    public PetResponseDto mapToDto(Pet pet) {
        Workspace workspace = pet.getWorkspace();

        return PetResponseDto.builder()
                .pid(pet.getPid())
                .name(pet.getName())
                .species(pet.getSpecies())
                .breed(pet.getBreed())
                .birthDate(pet.getBirthDate())
                .weight(pet.getWeight())
                .color(pet.getColor())
                .qrCode(pet.getQrCode())
                .emergencyContact(pet.getEmergencyContact())
                .imagesUrl(pet.getImagesUrl())
                .createdAt(pet.getCreatedAt())
                .workspaceUid(workspace != null ? workspace.getUid() : null)
                .workspaceName(workspace != null ? workspace.getName() : null)
                .planType(workspace != null ? workspace.getPlanType() : null)
                .workspaceStatus(workspace != null ? workspace.getStatus() : null)
                .build();
    }

    private PetResponsibleResponseDto mapToRespDto(PetResponsible resp) {
        return PetResponsibleResponseDto.builder()
                .userUid(resp.getUser().getUid())
                .userName(resp.getUser().getName())
                .userEmail(resp.getUser().getEmail())
                .accessLevel(resp.getAccessLevel())
                .responsibleRole(resp.getResponsibleRole())
                .createdAt(resp.getCreatedAt())
                .build();
    }
}
