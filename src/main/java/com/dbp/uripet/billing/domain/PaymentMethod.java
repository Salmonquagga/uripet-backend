package com.dbp.uripet.billing.domain;

import com.dbp.uripet.billing.domain.enums.PaymentMethodType;
import com.dbp.uripet.billing.domain.enums.PaymentProvider;
import com.dbp.uripet.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String uid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethodType type;

    @Column(name = "provider_token")
    private String providerToken;

    private String brand;
    private String last4;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
        this.active = true;
        if (this.uid == null) {
            this.uid = "PMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }
}