package com.dbp.uripet.workspace.repository;

import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.WorkspaceMember;
import com.dbp.uripet.workspace.domain.enums.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository
        extends JpaRepository<WorkspaceMember, Long> {

    /*
     * Todos los registros, incluidos los inactivos.
     * Útil para administración e historial.
     */
    List<WorkspaceMember> findByUser(User user);

    List<WorkspaceMember> findByWorkspace(Workspace workspace);

    Optional<WorkspaceMember> findByWorkspaceAndUser(
            Workspace workspace,
            User user
    );

    boolean existsByWorkspaceAndUser(
            Workspace workspace,
            User user
    );

    /*
     * Solo membresías activas.
     */
    List<WorkspaceMember> findByUserAndActiveTrue(User user);

    List<WorkspaceMember> findByWorkspaceAndActiveTrue(
            Workspace workspace
    );

    Optional<WorkspaceMember>
    findByWorkspaceAndUserAndActiveTrue(
            Workspace workspace,
            User user
    );

    boolean existsByWorkspaceAndUserAndActiveTrue(
            Workspace workspace,
            User user
    );

    long countByWorkspace(Workspace workspace);

    long countByWorkspaceAndActiveTrue(Workspace workspace);

    long countByWorkspaceAndRole(
            Workspace workspace,
            WorkspaceRole role
    );

    long countByWorkspaceAndRoleAndActiveTrue(
            Workspace workspace,
            WorkspaceRole role
    );
}