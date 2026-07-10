package com.dbp.uripet.billing.service;

import com.dbp.uripet.billing.domain.PaymentTransaction;
import com.dbp.uripet.billing.domain.Subscription;
import com.dbp.uripet.billing.domain.enums.PaymentProvider;
import com.dbp.uripet.billing.domain.enums.PaymentStatus;
import com.dbp.uripet.billing.domain.enums.SubscriptionStatus;
import com.dbp.uripet.billing.dto.CheckoutResponseDto;
import com.dbp.uripet.billing.dto.CreateCheckoutRequestDto;
import com.dbp.uripet.billing.dto.PaymentTransactionResponseDto;
import com.dbp.uripet.billing.dto.ReactivateSubscriptionRequestDto;
import com.dbp.uripet.billing.dto.SubscriptionResponseDto;
import com.dbp.uripet.billing.repository.PaymentTransactionRepository;
import com.dbp.uripet.billing.repository.SubscriptionRepository;
import com.dbp.uripet.config.error.ForbiddenException;
import com.dbp.uripet.config.error.InvalidOperationException;
import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.config.error.ValidationException;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.WorkspaceMember;
import com.dbp.uripet.workspace.domain.enums.PlanType;
import com.dbp.uripet.workspace.domain.enums.WorkspaceRole;
import com.dbp.uripet.workspace.domain.enums.WorkspaceStatus;
import com.dbp.uripet.workspace.repository.WorkspaceMemberRepository;
import com.dbp.uripet.workspace.repository.WorkspaceRepository;
import com.dbp.uripet.workspace.service.PlanAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BillingService {

    private static final Set<SubscriptionStatus>
            OPEN_SUBSCRIPTION_STATUSES =
            EnumSet.of(
                    SubscriptionStatus.PENDING_PAYMENT,
                    SubscriptionStatus.ACTIVE,
                    SubscriptionStatus.PAST_DUE
            );

    private static final Set<PaymentStatus>
            OPEN_PAYMENT_STATUSES =
            EnumSet.of(
                    PaymentStatus.PENDING,
                    PaymentStatus.SUCCESS
            );

    private final WorkspaceRepository workspaceRepository;

    private final WorkspaceMemberRepository
            workspaceMemberRepository;

    private final SubscriptionRepository
            subscriptionRepository;

    private final PaymentTransactionRepository
            paymentTransactionRepository;

    private final PlanPricingService
            planPricingService;

    private final PlanAccessService
            planAccessService;

    /**
     * Crea un nuevo workspace FAMILY o PREMIUM,
     * junto con su suscripción y transacción pendiente.
     *
     * No modifica ningún workspace existente.
     */
    @Transactional
    public CheckoutResponseDto createCheckout(
            CreateCheckoutRequestDto request,
            User currentUser
    ) {
        validateCurrentUser(currentUser);

        PlanType planType =
                parseGeneralCheckoutPlan(
                        request.getPlanType()
                );

        String workspaceName =
                normalizeWorkspaceName(
                        request.getWorkspaceName()
                );

        /*
         * Evita que un doble clic cree inmediatamente
         * otro grupo pendiente con el mismo nombre y plan.
         */
        Workspace duplicatedPendingWorkspace =
                workspaceRepository
                        .findFirstByOwnerAndNameIgnoreCaseAndPlanTypeAndStatusOrderByCreatedAtDesc(
                                currentUser,
                                workspaceName,
                                planType,
                                WorkspaceStatus.PENDING_PAYMENT
                        )
                        .orElse(null);

        if (duplicatedPendingWorkspace != null) {
            return getExistingPendingCheckout(
                    duplicatedPendingWorkspace
            );
        }

        BigDecimal amount =
                planPricingService.getMonthlyPrice(
                        planType
                );

        String currency =
                planPricingService.getCurrency(
                        planType
                );

        Workspace workspace =
                Workspace.builder()
                        .name(workspaceName)
                        .owner(currentUser)
                        .planType(planType)
                        .status(
                                WorkspaceStatus.PENDING_PAYMENT
                        )
                        .build();

        workspaceRepository.save(workspace);

        WorkspaceMember ownerMember =
                WorkspaceMember.builder()
                        .workspace(workspace)
                        .user(currentUser)
                        .role(WorkspaceRole.OWNER)
                        .active(true)
                        .build();

        workspaceMemberRepository.save(ownerMember);

        Subscription subscription =
                createPendingSubscription(
                        workspace,
                        planType,
                        amount,
                        currency
                );

        PaymentTransaction transaction =
                createPendingTransaction(
                        workspace,
                        subscription,
                        amount,
                        currency,
                        "Initial checkout for "
                                + planType.name()
                                + " plan"
                );

        return toCheckoutResponse(
                workspace,
                subscription,
                transaction,
                "Checkout created successfully"
        );
    }

    /**
     * Reactiva un workspace existente que fue
     * congelado, cancelado o quedó con pago vencido.
     */
    @Transactional
    public CheckoutResponseDto reactivateSubscription(
            String workspaceUid,
            ReactivateSubscriptionRequestDto request,
            User currentUser
    ) {
        validateCurrentUser(currentUser);

        Workspace workspace =
                getWorkspaceAndCheckBillingAccess(
                        workspaceUid,
                        currentUser
                );

        validateReactivatableWorkspace(workspace);

        /*
         * Si ya existe una suscripción pendiente,
         * devolvemos su checkout en lugar de duplicarla.
         */
        Subscription pendingSubscription =
                subscriptionRepository
                        .findTopByWorkspaceAndStatusInOrderByCreatedAtDesc(
                                workspace,
                                EnumSet.of(
                                        SubscriptionStatus.PENDING_PAYMENT
                                )
                        )
                        .orElse(null);

        if (pendingSubscription != null) {
            PaymentTransaction pendingTransaction =
                    paymentTransactionRepository
                            .findTopBySubscriptionOrderByCreatedAtDesc(
                                    pendingSubscription
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Pending payment transaction not found"
                                    )
                            );

            if (pendingTransaction.getStatus()
                    == PaymentStatus.PENDING) {

                workspace.setStatus(
                        WorkspaceStatus.PENDING_PAYMENT
                );

                workspaceRepository.save(workspace);

                return toCheckoutResponse(
                        workspace,
                        pendingSubscription,
                        pendingTransaction,
                        "Existing pending reactivation checkout returned"
                );
            }
        }

        PlanType planType =
                workspace.getPlanType();

        validateGeneralPaidPlan(planType);

        BigDecimal amount =
                planPricingService.getMonthlyPrice(
                        planType
                );

        String currency =
                planPricingService.getCurrency(
                        planType
                );

        workspace.setStatus(
                WorkspaceStatus.PENDING_PAYMENT
        );

        workspaceRepository.save(workspace);

        Subscription subscription =
                createPendingSubscription(
                        workspace,
                        planType,
                        amount,
                        currency
                );

        PaymentTransaction transaction =
                createPendingTransaction(
                        workspace,
                        subscription,
                        amount,
                        currency,
                        "Reactivation checkout for "
                                + planType.name()
                                + " plan"
                );

        return toCheckoutResponse(
                workspace,
                subscription,
                transaction,
                "Reactivation checkout created successfully"
        );
    }

    @Transactional(readOnly = true)
    public SubscriptionResponseDto
    getCurrentSubscriptionByWorkspace(
            String workspaceUid,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspaceAndCheckBillingAccess(
                        workspaceUid,
                        currentUser
                );

        Subscription subscription =
                subscriptionRepository
                        .findTopByWorkspaceOrderByCreatedAtDesc(
                                workspace
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Subscription not found for this workspace"
                                )
                        );

        return toSubscriptionResponse(subscription);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionResponseDto>
    getTransactionsByWorkspace(
            String workspaceUid,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspaceAndCheckBillingAccess(
                        workspaceUid,
                        currentUser
                );

        return paymentTransactionRepository
                .findByWorkspaceOrderByCreatedAtDesc(
                        workspace
                )
                .stream()
                .map(this::toTransactionResponse)
                .toList();
    }

    /**
     * Confirmación simulada desde Swagger.
     *
     * Solo el propietario del workspace puede usarla.
     */
    @Transactional
    public PaymentTransactionResponseDto
    confirmMockTransaction(
            String transactionUid,
            User currentUser
    ) {
        PaymentTransaction transaction =
                getTransaction(transactionUid);

        planAccessService.checkWorkspaceOwner(
                transaction.getWorkspace(),
                currentUser
        );

        return confirmTransaction(
                transactionUid,
                "MOCK-" + transactionUid
        );
    }

    /**
     * Fallo simulado desde Swagger.
     *
     * Solo el propietario del workspace puede usarlo.
     */
    @Transactional
    public PaymentTransactionResponseDto
    failMockTransaction(
            String transactionUid,
            String reason,
            User currentUser
    ) {
        PaymentTransaction transaction =
                getTransaction(transactionUid);

        planAccessService.checkWorkspaceOwner(
                transaction.getWorkspace(),
                currentUser
        );

        return failTransaction(
                transactionUid,
                reason
        );
    }

    /**
     * Método interno utilizado por:
     *
     * - Pago simulado.
     * - Webhook.
     *
     * Es idempotente: si ya fue confirmado,
     * devuelve la misma transacción.
     */
    @Transactional
    public PaymentTransactionResponseDto
    confirmTransaction(
            String transactionUid,
            String providerPaymentId
    ) {
        PaymentTransaction transaction =
                getTransaction(transactionUid);

        if (transaction.getStatus()
                == PaymentStatus.SUCCESS) {

            return toTransactionResponse(transaction);
        }

        if (transaction.getStatus()
                == PaymentStatus.CANCELLED
                || transaction.getStatus()
                == PaymentStatus.REFUNDED) {

            throw new InvalidOperationException(
                    "Cancelled or refunded transaction cannot be confirmed"
            );
        }

        Subscription subscription =
                transaction.getSubscription();

        if (subscription.getStatus()
                == SubscriptionStatus.CANCELLED
                && transaction.getStatus()
                != PaymentStatus.PENDING) {

            throw new InvalidOperationException(
                    "Cancelled subscription cannot be activated with this transaction"
            );
        }

        ZonedDateTime now =
                ZonedDateTime.now();

        transaction.setStatus(
                PaymentStatus.SUCCESS
        );

        transaction.setProviderPaymentId(
                StringUtils.hasText(providerPaymentId)
                        ? providerPaymentId.trim()
                        : "PAYMENT-" + transactionUid
        );

        transaction.setPaidAt(now);
        transaction.setFailureReason(null);

        subscription.setStatus(
                SubscriptionStatus.ACTIVE
        );

        if (subscription.getStartedAt() == null) {
            subscription.setStartedAt(now);
        }

        subscription.setNextBillingAt(
                now.plusMonths(1)
        );

        subscription.setCancelledAt(null);

        Workspace workspace =
                transaction.getWorkspace();

        if (workspace.getPlanType()
                == PlanType.FREE) {

            throw new InvalidOperationException(
                    "Personal workspace cannot be activated through billing"
            );
        }

        workspace.setPlanType(
                subscription.getPlanType()
        );

        workspace.setStatus(
                WorkspaceStatus.ACTIVE
        );

        workspaceRepository.save(workspace);
        subscriptionRepository.save(subscription);
        paymentTransactionRepository.save(transaction);

        return toTransactionResponse(transaction);
    }

    /**
     * Marca una transacción como fallida.
     *
     * Una transacción exitosa no puede convertirse
     * posteriormente en fallida.
     */
    @Transactional
    public PaymentTransactionResponseDto
    failTransaction(
            String transactionUid,
            String reason
    ) {
        PaymentTransaction transaction =
                getTransaction(transactionUid);

        if (transaction.getStatus()
                == PaymentStatus.SUCCESS) {

            throw new InvalidOperationException(
                    "Successful transaction cannot be marked as failed"
            );
        }

        if (transaction.getStatus()
                == PaymentStatus.FAILED) {

            return toTransactionResponse(transaction);
        }

        if (transaction.getStatus()
                == PaymentStatus.CANCELLED
                || transaction.getStatus()
                == PaymentStatus.REFUNDED) {

            throw new InvalidOperationException(
                    "Cancelled or refunded transaction cannot be marked as failed"
            );
        }

        transaction.setStatus(
                PaymentStatus.FAILED
        );

        transaction.setFailureReason(
                StringUtils.hasText(reason)
                        ? reason.trim()
                        : "Payment failed"
        );

        Subscription subscription =
                transaction.getSubscription();

        subscription.setStatus(
                SubscriptionStatus.PAST_DUE
        );

        Workspace workspace =
                transaction.getWorkspace();

        /*
         * Si nunca se pagó, queda pendiente de resolver.
         * PAST_DUE permitirá mostrar el aviso de pago.
         */
        workspace.setStatus(
                WorkspaceStatus.PAST_DUE
        );

        workspaceRepository.save(workspace);
        subscriptionRepository.save(subscription);
        paymentTransactionRepository.save(transaction);

        return toTransactionResponse(transaction);
    }

    /**
     * Cancela la suscripción sin eliminar el grupo.
     *
     * El workspace pasa a FROZEN y conserva:
     *
     * - Mascotas.
     * - Miembros.
     * - Historial.
     * - Configuración.
     */
    @Transactional
    public SubscriptionResponseDto
    cancelSubscription(
            String workspaceUid,
            User currentUser
    ) {
        Workspace workspace =
                getWorkspaceAndCheckBillingAccess(
                        workspaceUid,
                        currentUser
                );

        if (workspace.getPlanType()
                == PlanType.FREE) {

            throw new InvalidOperationException(
                    "Personal workspace cannot be cancelled"
            );
        }

        Subscription subscription =
                subscriptionRepository
                        .findTopByWorkspaceOrderByCreatedAtDesc(
                                workspace
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Subscription not found for this workspace"
                                )
                        );

        if (subscription.getStatus()
                == SubscriptionStatus.CANCELLED
                && workspace.getStatus()
                == WorkspaceStatus.FROZEN) {

            return toSubscriptionResponse(subscription);
        }

        if (subscription.getStatus()
                != SubscriptionStatus.ACTIVE
                && subscription.getStatus()
                != SubscriptionStatus.PAST_DUE
                && subscription.getStatus()
                != SubscriptionStatus.PENDING_PAYMENT) {

            throw new InvalidOperationException(
                    "Subscription cannot be cancelled from its current status"
            );
        }

        ZonedDateTime now =
                ZonedDateTime.now();

        subscription.setStatus(
                SubscriptionStatus.CANCELLED
        );

        subscription.setCancelledAt(now);

        workspace.setStatus(
                WorkspaceStatus.FROZEN
        );

        /*
         * Si había una transacción pendiente,
         * se cancela para que no pueda activarse
         * accidentalmente después.
         */
        paymentTransactionRepository
                .findTopByWorkspaceAndStatusOrderByCreatedAtDesc(
                        workspace,
                        PaymentStatus.PENDING
                )
                .ifPresent(transaction -> {
                    transaction.setStatus(
                            PaymentStatus.CANCELLED
                    );

                    transaction.setFailureReason(
                            "Subscription cancelled by workspace owner"
                    );

                    paymentTransactionRepository.save(
                            transaction
                    );
                });

        workspaceRepository.save(workspace);
        subscriptionRepository.save(subscription);

        return toSubscriptionResponse(subscription);
    }

    private Subscription createPendingSubscription(
            Workspace workspace,
            PlanType planType,
            BigDecimal amount,
            String currency
    ) {
        if (subscriptionRepository
                .existsByWorkspaceAndStatusIn(
                        workspace,
                        OPEN_SUBSCRIPTION_STATUSES
                )) {

            throw new InvalidOperationException(
                    "Workspace already has an open subscription"
            );
        }

        Subscription subscription =
                Subscription.builder()
                        .workspace(workspace)
                        .planType(planType)
                        .status(
                                SubscriptionStatus.PENDING_PAYMENT
                        )
                        .amount(amount)
                        .currency(currency)
                        .build();

        return subscriptionRepository.save(
                subscription
        );
    }

    private PaymentTransaction createPendingTransaction(
            Workspace workspace,
            Subscription subscription,
            BigDecimal amount,
            String currency,
            String description
    ) {
        if (paymentTransactionRepository
                .existsBySubscriptionAndStatus(
                        subscription,
                        PaymentStatus.PENDING
                )) {

            throw new InvalidOperationException(
                    "Subscription already has a pending payment transaction"
            );
        }

        PaymentTransaction transaction =
                PaymentTransaction.builder()
                        .workspace(workspace)
                        .subscription(subscription)
                        .provider(
                                PaymentProvider.MOCK
                        )
                        .status(
                                PaymentStatus.PENDING
                        )
                        .amount(amount)
                        .currency(currency)
                        .description(description)
                        .build();

        return paymentTransactionRepository.save(
                transaction
        );
    }

    private CheckoutResponseDto
    getExistingPendingCheckout(
            Workspace workspace
    ) {
        Subscription subscription =
                subscriptionRepository
                        .findTopByWorkspaceAndStatusInOrderByCreatedAtDesc(
                                workspace,
                                EnumSet.of(
                                        SubscriptionStatus.PENDING_PAYMENT
                                )
                        )
                        .orElse(null);

        if (subscription == null) {
            throw new InvalidOperationException(
                    "A pending workspace with this name already exists"
            );
        }

        PaymentTransaction transaction =
                paymentTransactionRepository
                        .findTopBySubscriptionOrderByCreatedAtDesc(
                                subscription
                        )
                        .orElse(null);

        if (transaction == null
                || transaction.getStatus()
                != PaymentStatus.PENDING) {

            throw new InvalidOperationException(
                    "A pending workspace with this name already exists but has no valid checkout"
            );
        }

        return toCheckoutResponse(
                workspace,
                subscription,
                transaction,
                "Existing pending checkout returned"
        );
    }

    private Workspace
    getWorkspaceAndCheckBillingAccess(
            String workspaceUid,
            User currentUser
    ) {
        if (!StringUtils.hasText(workspaceUid)) {
            throw new ValidationException(
                    "Workspace UID is required"
            );
        }

        Workspace workspace =
                workspaceRepository
                        .findByUid(
                                workspaceUid.trim()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Workspace not found"
                                )
                        );

        planAccessService.checkWorkspaceOwner(
                workspace,
                currentUser
        );

        return workspace;
    }

    private PaymentTransaction getTransaction(
            String transactionUid
    ) {
        if (!StringUtils.hasText(transactionUid)) {
            throw new ValidationException(
                    "Transaction UID is required"
            );
        }

        return paymentTransactionRepository
                .findByUid(transactionUid.trim())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment transaction not found"
                        )
                );
    }

    private PlanType parseGeneralCheckoutPlan(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            throw new ValidationException(
                    "Plan type is required"
            );
        }

        final PlanType planType;

        try {
            planType = PlanType.valueOf(
                    value.trim().toUpperCase()
            );
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "Invalid plan type: " + value
            );
        }

        validateGeneralPaidPlan(planType);

        return planType;
    }

    private void validateGeneralPaidPlan(
            PlanType planType
    ) {
        if (!planPricingService
                .isGeneralCheckoutPlan(planType)) {

            if (planType == PlanType.FREE) {
                throw new InvalidOperationException(
                        "FREE plan does not require checkout"
                );
            }

            throw new InvalidOperationException(
                    "Only FAMILY and PREMIUM plans are available through general checkout"
            );
        }
    }

    private void validateReactivatableWorkspace(
            Workspace workspace
    ) {
        validateGeneralPaidPlan(
                workspace.getPlanType()
        );

        if (workspace.getStatus()
                == WorkspaceStatus.ACTIVE) {

            throw new InvalidOperationException(
                    "Workspace subscription is already active"
            );
        }

        if (workspace.getStatus()
                != WorkspaceStatus.FROZEN
                && workspace.getStatus()
                != WorkspaceStatus.CANCELLED
                && workspace.getStatus()
                != WorkspaceStatus.PAST_DUE
                && workspace.getStatus()
                != WorkspaceStatus.PENDING_PAYMENT) {

            throw new InvalidOperationException(
                    "Workspace cannot be reactivated from its current status"
            );
        }
    }

    private void validateCurrentUser(
            User currentUser
    ) {
        if (currentUser == null) {
            throw new ForbiddenException(
                    "Authenticated user is required"
            );
        }
    }

    private String normalizeWorkspaceName(
            String value
    ) {
        if (!StringUtils.hasText(value)) {
            throw new ValidationException(
                    "Workspace name is required"
            );
        }

        String normalized =
                value.trim().replaceAll("\\s+", " ");

        if (normalized.length() < 2
                || normalized.length() > 80) {

            throw new ValidationException(
                    "Workspace name must contain between 2 and 80 characters"
            );
        }

        return normalized;
    }

    private CheckoutResponseDto toCheckoutResponse(
            Workspace workspace,
            Subscription subscription,
            PaymentTransaction transaction,
            String message
    ) {
        return CheckoutResponseDto.builder()
                .workspaceUid(
                        workspace.getUid()
                )
                .workspaceName(
                        workspace.getName()
                )
                .subscriptionUid(
                        subscription.getUid()
                )
                .transactionUid(
                        transaction.getUid()
                )
                .planType(
                        subscription
                                .getPlanType()
                                .name()
                )
                .subscriptionStatus(
                        subscription
                                .getStatus()
                                .name()
                )
                .paymentStatus(
                        transaction
                                .getStatus()
                                .name()
                )
                .amount(
                        transaction.getAmount()
                )
                .currency(
                        transaction.getCurrency()
                )
                .paymentUrl(
                        "mock://checkout/"
                                + transaction.getUid()
                )
                .message(message)
                .build();
    }

    private SubscriptionResponseDto
    toSubscriptionResponse(
            Subscription subscription
    ) {
        Workspace workspace =
                subscription.getWorkspace();

        return SubscriptionResponseDto.builder()
                .uid(subscription.getUid())
                .workspaceUid(
                        workspace.getUid()
                )
                .workspaceName(
                        workspace.getName()
                )
                .planType(
                        subscription
                                .getPlanType()
                                .name()
                )
                .status(
                        subscription
                                .getStatus()
                                .name()
                )
                .amount(
                        subscription.getAmount()
                )
                .currency(
                        subscription.getCurrency()
                )
                .startedAt(
                        subscription.getStartedAt()
                )
                .nextBillingAt(
                        subscription.getNextBillingAt()
                )
                .cancelledAt(
                        subscription.getCancelledAt()
                )
                .createdAt(
                        subscription.getCreatedAt()
                )
                .build();
    }

    private PaymentTransactionResponseDto
    toTransactionResponse(
            PaymentTransaction transaction
    ) {
        return PaymentTransactionResponseDto.builder()
                .uid(transaction.getUid())
                .workspaceUid(
                        transaction
                                .getWorkspace()
                                .getUid()
                )
                .subscriptionUid(
                        transaction
                                .getSubscription()
                                .getUid()
                )
                .provider(
                        transaction
                                .getProvider()
                                .name()
                )
                .status(
                        transaction
                                .getStatus()
                                .name()
                )
                .providerPaymentId(
                        transaction
                                .getProviderPaymentId()
                )
                .amount(
                        transaction.getAmount()
                )
                .currency(
                        transaction.getCurrency()
                )
                .description(
                        transaction.getDescription()
                )
                .failureReason(
                        transaction.getFailureReason()
                )
                .createdAt(
                        transaction.getCreatedAt()
                )
                .paidAt(
                        transaction.getPaidAt()
                )
                .build();
    }
}