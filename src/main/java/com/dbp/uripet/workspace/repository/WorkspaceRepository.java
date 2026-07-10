package com.dbp.uripet.workspace.repository;

import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.enums.PlanType;
import com.dbp.uripet.workspace.domain.enums.WorkspaceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceRepository
        extends JpaRepository<Workspace, Long> {

    Optional<Workspace> findByUid(
            String uid
    );

    List<Workspace> findByOwner(
            User owner
    );

    Optional<Workspace> findByOwnerAndPlanType(
            User owner,
            PlanType planType
    );

    boolean existsByOwnerAndPlanType(
            User owner,
            PlanType planType
    );

    Optional<Workspace>
    findFirstByOwnerAndNameIgnoreCaseAndPlanTypeAndStatusOrderByCreatedAtDesc(
            User owner,
            String name,
            PlanType planType,
            WorkspaceStatus status
    );
}