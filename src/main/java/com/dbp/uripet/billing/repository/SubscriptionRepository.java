package com.dbp.uripet.billing.repository;

import com.dbp.uripet.billing.domain.Subscription;
import com.dbp.uripet.billing.domain.enums.SubscriptionStatus;
import com.dbp.uripet.workspace.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository
        extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUid(
            String uid
    );

    Optional<Subscription>
    findTopByWorkspaceOrderByCreatedAtDesc(
            Workspace workspace
    );

    List<Subscription>
    findByWorkspaceOrderByCreatedAtDesc(
            Workspace workspace
    );

    Optional<Subscription>
    findTopByWorkspaceAndStatusInOrderByCreatedAtDesc(
            Workspace workspace,
            Collection<SubscriptionStatus> statuses
    );

    boolean existsByWorkspaceAndStatus(
            Workspace workspace,
            SubscriptionStatus status
    );

    boolean existsByWorkspaceAndStatusIn(
            Workspace workspace,
            Collection<SubscriptionStatus> statuses
    );
}