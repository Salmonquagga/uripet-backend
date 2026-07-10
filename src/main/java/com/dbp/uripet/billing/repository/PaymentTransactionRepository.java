package com.dbp.uripet.billing.repository;

import com.dbp.uripet.billing.domain.PaymentTransaction;
import com.dbp.uripet.billing.domain.Subscription;
import com.dbp.uripet.billing.domain.enums.PaymentStatus;
import com.dbp.uripet.workspace.domain.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByUid(
            String uid
    );

    List<PaymentTransaction>
    findByWorkspaceOrderByCreatedAtDesc(
            Workspace workspace
    );

    Optional<PaymentTransaction>
    findTopBySubscriptionOrderByCreatedAtDesc(
            Subscription subscription
    );

    Optional<PaymentTransaction>
    findTopByWorkspaceAndStatusOrderByCreatedAtDesc(
            Workspace workspace,
            PaymentStatus status
    );

    boolean existsBySubscriptionAndStatus(
            Subscription subscription,
            PaymentStatus status
    );

    boolean existsByWorkspaceAndStatusIn(
            Workspace workspace,
            Collection<PaymentStatus> statuses
    );
}