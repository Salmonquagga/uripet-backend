package com.dbp.uripet.billing.domain;

import com.dbp.uripet.billing.domain.enums.PaymentProvider;
import com.dbp.uripet.billing.domain.enums.PaymentStatus;
import com.dbp.uripet.workspace.domain.Workspace;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String uid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "provider_payment_id")
    private String providerPaymentId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    private String description;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "paid_at")
    private ZonedDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
        if (this.uid == null) {
            this.uid = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
        if (this.provider == null) {
            this.provider = PaymentProvider.MOCK;
        }
        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "PEN";
        }
    }
}