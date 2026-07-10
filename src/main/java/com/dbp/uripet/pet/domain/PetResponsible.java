package com.dbp.uripet.pet.domain;

import com.dbp.uripet.pet.domain.enums.AccessLevel;
import com.dbp.uripet.pet.domain.enums.ResponsibleRole;
import com.dbp.uripet.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "pet_responsibles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetResponsible {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessLevel accessLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResponsibleRole responsibleRole;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();
    }
}
