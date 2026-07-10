package com.dbp.uripet.pet.service;

import com.dbp.uripet.config.error.DuplicateResourceException;
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
import com.dbp.uripet.workspace.domain.enums.WorkspaceStatus;
import com.dbp.uripet.workspace.repository.WorkspaceMemberRepository;
import com.dbp.uripet.workspace.repository.WorkspaceRepository;
import com.dbp.uripet.workspace.service.PlanAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;

    private final PetResponsibleRepository
            petResponsibleRepository;

    private final UserService userService;

    private final UserRepository userRepository;

    private final ApplicationEventPublisher
            eventPublisher;

    private final ImageStorageService
            imageStorageService;

    private final WorkspaceRepository
            workspaceRepository;

    private final WorkspaceMemberRepository
            workspaceMemberRepository;

    private final PlanAccessService
            planAccessService;

    /*
     * =====================================================
     * MASCOTAS
     * =====================================================
     */

    @Transactional
    public PetResponseDto createPet(
            PetRequestDto request
    ) {
        User currentUser =
                userService.getAuthenticatedUser();

        Workspace workspace =
                resolveWorkspaceForCreate(
                        request.getWorkspaceUid(),
                        currentUser
                );

        /*
         * Valida:
         * - workspace activo;
         * - rol OWNER o ADMIN;
         * - límite de mascotas según plan.
         */
        planAccessService.checkCanCreatePet(
                workspace,
                currentUser
        );

        Pet pet = Pet.builder()
                .workspace(workspace)
                .name(cleanText(request.getName()))
                .species(cleanText(request.getSpecies()))
                .breed(cleanText(request.getBreed()))
                .birthDate(request.getBirthDate())
                .weight(request.getWeight())
                .color(cleanText(request.getColor()))
                .emergencyContact(
                        cleanText(
                                request.getEmergencyContact()
                        )
                )
                .build();

        petRepository.save(pet);

        List<String> imagesUrl =
                resolveImages(
                        pet,
                        request,
                        null
                );

        pet.setImagesUrl(imagesUrl);

        Pet savedPet =
                petRepository.save(pet);

        /*
         * El creador se registra como responsable
         * principal de la mascota.
         *
         * Ya pertenece al workspace, por lo tanto
         * no se crea una membresía adicional.
         */
        if (!petResponsibleRepository
                .existsByPetAndUser(
                        savedPet,
                        currentUser
                )) {

            PetResponsible responsible =
                    PetResponsible.builder()
                            .pet(savedPet)
                            .user(currentUser)
                            .accessLevel(
                                    AccessLevel.EDITOR
                            )
                            .responsibleRole(
                                    ResponsibleRole.OWNER
                            )
                            .build();

            petResponsibleRepository.save(
                    responsible
            );
        }

        return mapToDto(savedPet);
    }

    @Transactional(readOnly = true)
    public PetResponseDto getPet(
            String pid
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        /*
         * El detalle completo no se entrega
         * para grupos congelados.
         *
         * En ese caso se usa:
         * GET /workspaces/{workspaceUid}/preview
         */
        planAccessService.checkCanViewPets(
                pet.getWorkspace(),
                currentUser
        );

        planAccessService.checkWorkspaceActive(
                pet.getWorkspace()
        );

        return mapToDto(pet);
    }

    /*
     * Se conserva por compatibilidad interna.
     *
     * El endpoint público actual utiliza
     * PetPrivacyService, que filtra los datos.
     */
    @Transactional(readOnly = true)
    public PetResponseDto getPublicPet(
            String pid
    ) {
        return mapToDto(
                findPet(pid)
        );
    }

    @Transactional
    public PetResponseDto updatePet(
            String pid,
            PetRequestDto request
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        planAccessService.checkCanEditPet(
                pet.getWorkspace(),
                currentUser
        );

        if (request.getName() != null) {
            pet.setName(
                    cleanText(request.getName())
            );
        }

        if (request.getSpecies() != null) {
            pet.setSpecies(
                    cleanText(request.getSpecies())
            );
        }

        if (request.getBreed() != null) {
            pet.setBreed(
                    cleanText(request.getBreed())
            );
        }

        if (request.getBirthDate() != null) {
            pet.setBirthDate(
                    request.getBirthDate()
            );
        }

        if (request.getWeight() != null) {
            pet.setWeight(
                    request.getWeight()
            );
        }

        if (request.getColor() != null) {
            pet.setColor(
                    cleanText(request.getColor())
            );
        }

        if (request.getEmergencyContact() != null) {
            pet.setEmergencyContact(
                    cleanText(
                            request.getEmergencyContact()
                    )
            );
        }

        List<String> imagesUrl =
                resolveImages(
                        pet,
                        request,
                        pet.getImagesUrl()
                );

        pet.setImagesUrl(imagesUrl);

        return mapToDto(
                petRepository.save(pet)
        );
    }

    @Transactional
    public void deletePet(
            String pid
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        planAccessService.checkCanDeletePet(
                pet.getWorkspace(),
                currentUser
        );

        /*
         * Los registros médicos y responsables
         * se eliminan por cascade/orphanRemoval
         * configurado en Pet.
         */
        petRepository.delete(pet);
    }

    @Transactional(readOnly = true)
    public Page<PetResponseDto> getMyPets(
            int page,
            int size,
            String search,
            String workspaceUid
    ) {
        User currentUser =
                userService.getAuthenticatedUser();

        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.min(
                                Math.max(size, 1),
                                100
                        ),
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        String cleanSearch =
                StringUtils.hasText(search)
                        ? search.trim()
                        : null;

        Page<Pet> pets;

        if (StringUtils.hasText(workspaceUid)) {
            Workspace workspace =
                    findWorkspace(workspaceUid);

            planAccessService.checkCanViewPets(
                    workspace,
                    currentUser
            );

            /*
             * Si está congelado, el frontend
             * debe consumir el preview seguro.
             */
            planAccessService.checkWorkspaceActive(
                    workspace
            );

            if (cleanSearch != null) {
                pets = petRepository
                        .findByWorkspaceAndNameContainingIgnoreCase(
                                workspace,
                                cleanSearch,
                                pageable
                        );
            } else {
                pets = petRepository
                        .findByWorkspace(
                                workspace,
                                pageable
                        );
            }

        } else {
            List<Workspace> activeWorkspaces =
                    getAccessibleActiveWorkspaces(
                            currentUser
                    );

            if (activeWorkspaces.isEmpty()) {
                return Page.empty(pageable);
            }

            if (cleanSearch != null) {
                pets = petRepository
                        .findByWorkspaceInAndNameContainingIgnoreCase(
                                activeWorkspaces,
                                cleanSearch,
                                pageable
                        );
            } else {
                pets = petRepository
                        .findByWorkspaceIn(
                                activeWorkspaces,
                                pageable
                        );
            }
        }

        return pets.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<PetResponseDto> getPetsByUser(
            String uid
    ) {
        User requestedUser =
                userRepository
                        .findByUid(uid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        User currentUser =
                userService.getAuthenticatedUser();

        /*
         * Este endpoint no debe utilizarse para
         * consultar libremente las mascotas de
         * cualquier usuario.
         */
        if (!requestedUser.getId()
                .equals(currentUser.getId())) {

            throw new InvalidOperationException(
                    "You can only list pets from your own accessible workspaces"
            );
        }

        List<Workspace> activeWorkspaces =
                getAccessibleActiveWorkspaces(
                        currentUser
                );

        if (activeWorkspaces.isEmpty()) {
            return List.of();
        }

        Pageable pageable =
                PageRequest.of(
                        0,
                        100,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        return petRepository
                .findByWorkspaceIn(
                        activeWorkspaces,
                        pageable
                )
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PetQrDataDto getQrData(
            String pid
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        planAccessService.checkCanViewPets(
                pet.getWorkspace(),
                currentUser
        );

        return PetQrDataDto.builder()
                .pid(pet.getPid())
                .isPublic(true)
                .build();
    }

    /*
     * =====================================================
     * RESPONSABLES
     * =====================================================
     */

    @Transactional
    public PetResponsibleResponseDto addResponsible(
            String pid,
            PetResponsibleRequestDto request
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        Workspace workspace =
                pet.getWorkspace();

        planAccessService.checkCanEditPet(
                workspace,
                currentUser
        );

        User targetUser =
                userRepository
                        .findByUid(
                                request.getUserUid()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Target user not found"
                                )
                        );

        /*
         * Ya no se crea automáticamente
         * una membresía al workspace.
         *
         * Primero debe aceptar una invitación
         * y ser miembro activo del grupo.
         */
        WorkspaceMember targetMembership =
                workspaceMemberRepository
                        .findByWorkspaceAndUserAndActiveTrue(
                                workspace,
                                targetUser
                        )
                        .orElseThrow(() ->
                                new InvalidOperationException(
                                        "User must be an active workspace member before becoming responsible for a pet"
                                )
                        );

        if (petResponsibleRepository
                .existsByPetAndUser(
                        pet,
                        targetUser
                )) {

            throw new DuplicateResourceException(
                    "User is already responsible for this pet"
            );
        }

        validateResponsibleAssignment(
                workspace,
                targetMembership,
                request
        );

        PetResponsible responsible =
                PetResponsible.builder()
                        .pet(pet)
                        .user(targetUser)
                        .accessLevel(
                                request.getAccessLevel()
                        )
                        .responsibleRole(
                                request.getResponsibleRole()
                        )
                        .build();

        PetResponsible savedResponsible =
                petResponsibleRepository.save(
                        responsible
                );

        eventPublisher.publishEvent(
                new UserAddedToPetEvent(
                        targetUser.getEmail(),
                        pet.getName()
                )
        );

        return mapToResponsibleDto(
                savedResponsible
        );
    }

    @Transactional(readOnly = true)
    public List<PetResponsibleResponseDto>
    getResponsibles(
            String pid
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        planAccessService.checkCanViewPets(
                pet.getWorkspace(),
                currentUser
        );

        planAccessService.checkWorkspaceActive(
                pet.getWorkspace()
        );

        return petResponsibleRepository
                .findByPet(pet)
                .stream()
                .map(this::mapToResponsibleDto)
                .toList();
    }

    @Transactional
    public void removeResponsible(
            String pid,
            String userUid
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        Workspace workspace =
                pet.getWorkspace();

        planAccessService.checkCanEditPet(
                workspace,
                currentUser
        );

        User targetUser =
                userRepository
                        .findByUid(userUid)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Target user not found"
                                )
                        );

        PetResponsible responsible =
                petResponsibleRepository
                        .findByPetAndUser(
                                pet,
                                targetUser
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Responsible not found"
                                )
                        );

        if (responsible.getResponsibleRole()
                == ResponsibleRole.OWNER) {

            int ownerCount =
                    petResponsibleRepository
                            .countByPetAndResponsibleRole(
                                    pet,
                                    ResponsibleRole.OWNER
                            );

            if (ownerCount <= 1) {
                throw new InvalidOperationException(
                        "Cannot remove the last pet owner"
                );
            }
        }

        /*
         * Solo se elimina la responsabilidad
         * sobre esta mascota.
         *
         * La membresía del workspace se mantiene.
         */
        petResponsibleRepository.delete(
                responsible
        );
    }

    /*
     * =====================================================
     * IMÁGENES
     * =====================================================
     */

    @Transactional
    public PetResponseDto removeImage(
            String pid,
            String imageUrl
    ) {
        Pet pet = findPet(pid);

        User currentUser =
                userService.getAuthenticatedUser();

        planAccessService.checkCanEditPet(
                pet.getWorkspace(),
                currentUser
        );

        if (!StringUtils.hasText(imageUrl)) {
            throw new InvalidOperationException(
                    "Image URL is required"
            );
        }

        List<String> currentImages =
                pet.getImagesUrl() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(
                        pet.getImagesUrl()
                );

        boolean removed =
                currentImages.removeIf(
                        image ->
                                imageUrl.equals(image)
                );

        if (!removed) {
            throw new ResourceNotFoundException(
                    "Image not found"
            );
        }

        pet.setImagesUrl(currentImages);

        return mapToDto(
                petRepository.save(pet)
        );
    }

    /*
     * =====================================================
     * MÉTODOS PRIVADOS
     * =====================================================
     */

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

    private Workspace findWorkspace(
            String workspaceUid
    ) {
        return workspaceRepository
                .findByUid(workspaceUid.trim())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Workspace not found"
                        )
                );
    }

    private Workspace resolveWorkspaceForCreate(
            String workspaceUid,
            User currentUser
    ) {
        if (StringUtils.hasText(workspaceUid)) {
            Workspace workspace =
                    findWorkspace(workspaceUid);

            planAccessService
                    .checkCanAccessWorkspace(
                            workspace,
                            currentUser
                    );

            return workspace;
        }

        return getPersonalWorkspace(
                currentUser
        );
    }

    private Workspace getPersonalWorkspace(
            User currentUser
    ) {
        return workspaceMemberRepository
                .findByUserAndActiveTrue(
                        currentUser
                )
                .stream()
                .map(
                        WorkspaceMember::getWorkspace
                )
                .filter(workspace ->
                        workspace.getPlanType()
                                == PlanType.FREE
                )
                .filter(workspace ->
                        workspace.getOwner() != null
                                && workspace
                                .getOwner()
                                .getId()
                                .equals(
                                        currentUser.getId()
                                )
                )
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Personal workspace not found"
                        )
                );
    }

    private List<Workspace>
    getAccessibleActiveWorkspaces(
            User currentUser
    ) {
        return workspaceMemberRepository
                .findByUserAndActiveTrue(
                        currentUser
                )
                .stream()
                .map(
                        WorkspaceMember::getWorkspace
                )
                .filter(workspace ->
                        workspace.getStatus()
                                == WorkspaceStatus.ACTIVE
                )
                .distinct()
                .toList();
    }

    private void validateResponsibleAssignment(
            Workspace workspace,
            WorkspaceMember targetMembership,
            PetResponsibleRequestDto request
    ) {
        if (request.getResponsibleRole()
                == ResponsibleRole.OWNER
                && targetMembership.getRole()
                == com.dbp.uripet.workspace.domain.enums
                .WorkspaceRole.MEMBER) {

            throw new InvalidOperationException(
                    "A workspace MEMBER cannot be assigned as pet OWNER"
            );
        }

        if (request.getAccessLevel()
                == AccessLevel.EDITOR
                && targetMembership.getRole()
                == com.dbp.uripet.workspace.domain.enums
                .WorkspaceRole.MEMBER) {

            throw new InvalidOperationException(
                    "A workspace MEMBER cannot receive EDITOR access to a pet"
            );
        }

        if (workspace.getStatus()
                != WorkspaceStatus.ACTIVE) {

            throw new InvalidOperationException(
                    "Workspace must be active"
            );
        }
    }

    private List<String> resolveImages(
            Pet pet,
            PetRequestDto request,
            List<String> existingImages
    ) {
        List<String> finalImages =
                new ArrayList<>();

        if (request.getImagesUrl() != null) {
            for (String imageUrl
                    : request.getImagesUrl()) {

                if (StringUtils.hasText(imageUrl)
                        && !finalImages.contains(
                        imageUrl.trim()
                )) {
                    finalImages.add(
                            imageUrl.trim()
                    );
                }
            }

        } else if (existingImages != null) {
            for (String imageUrl
                    : existingImages) {

                if (StringUtils.hasText(imageUrl)
                        && !finalImages.contains(
                        imageUrl
                )) {
                    finalImages.add(imageUrl);
                }
            }
        }

        if (request.getImages() != null
                && !request.getImages().isEmpty()) {

            List<String> uploadedImages =
                    imageStorageService
                            .storePetImages(
                                    pet.getPid(),
                                    request.getImages()
                            );

            for (String uploadedImage
                    : uploadedImages) {

                if (StringUtils.hasText(
                        uploadedImage
                )
                        && !finalImages.contains(
                        uploadedImage
                )) {

                    finalImages.add(
                            uploadedImage
                    );
                }
            }
        }

        if (finalImages.size() > 3) {
            throw new InvalidOperationException(
                    "A pet can have at most 3 images"
            );
        }

        return finalImages;
    }

    private String cleanText(
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

    public PetResponseDto mapToDto(
            Pet pet
    ) {
        Workspace workspace =
                pet.getWorkspace();

        return PetResponseDto.builder()
                .pid(pet.getPid())
                .name(pet.getName())
                .species(pet.getSpecies())
                .breed(pet.getBreed())
                .birthDate(pet.getBirthDate())
                .weight(pet.getWeight())
                .color(pet.getColor())
                .qrCode(pet.getQrCode())
                .emergencyContact(
                        pet.getEmergencyContact()
                )
                .imagesUrl(
                        pet.getImagesUrl()
                )
                .createdAt(
                        pet.getCreatedAt()
                )
                .workspaceUid(
                        workspace != null
                                ? workspace.getUid()
                                : null
                )
                .workspaceName(
                        workspace != null
                                ? workspace.getName()
                                : null
                )
                .planType(
                        workspace != null
                                ? workspace.getPlanType()
                                : null
                )
                .workspaceStatus(
                        workspace != null
                                ? workspace.getStatus()
                                : null
                )
                .build();
    }

    private PetResponsibleResponseDto
    mapToResponsibleDto(
            PetResponsible responsible
    ) {
        return PetResponsibleResponseDto.builder()
                .userUid(
                        responsible
                                .getUser()
                                .getUid()
                )
                .userName(
                        responsible
                                .getUser()
                                .getName()
                )
                .userEmail(
                        responsible
                                .getUser()
                                .getEmail()
                )
                .accessLevel(
                        responsible
                                .getAccessLevel()
                )
                .responsibleRole(
                        responsible
                                .getResponsibleRole()
                )
                .createdAt(
                        responsible
                                .getCreatedAt()
                )
                .build();
    }
}