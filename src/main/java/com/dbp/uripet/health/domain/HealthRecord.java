package com.dbp.uripet.health.domain;

import com.dbp.uripet.pet.domain.Pet;
import com.dbp.uripet.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "health_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthRecord {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "pet_id",
            nullable = false
    )
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "created_by_id",
            nullable = false
    )
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HealthRecordType type;

    private String title;

    @Column(length = 1000)
    private String description;

    private LocalDate date;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt =
                ZonedDateTime.now();
    }
}