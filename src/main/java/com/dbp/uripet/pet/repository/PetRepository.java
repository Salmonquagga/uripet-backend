package com.dbp.uripet.pet.repository;

import com.dbp.uripet.pet.domain.Pet;
import com.dbp.uripet.workspace.domain.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetRepository
        extends JpaRepository<Pet, Long> {

    Optional<Pet> findByPid(
            String pid
    );

    Optional<Pet> findByQrCode(
            UUID qrCode
    );

    long countByWorkspace(
            Workspace workspace
    );

    List<Pet> findByWorkspaceOrderByCreatedAtAsc(
            Workspace workspace
    );

    Page<Pet> findByWorkspace(
            Workspace workspace,
            Pageable pageable
    );

    Page<Pet>
    findByWorkspaceAndNameContainingIgnoreCase(
            Workspace workspace,
            String name,
            Pageable pageable
    );

    Page<Pet> findByWorkspaceIn(
            List<Workspace> workspaces,
            Pageable pageable
    );

    Page<Pet>
    findByWorkspaceInAndNameContainingIgnoreCase(
            List<Workspace> workspaces,
            String name,
            Pageable pageable
    );
}