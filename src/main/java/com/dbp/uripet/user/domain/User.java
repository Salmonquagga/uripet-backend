package com.dbp.uripet.user.domain;

import com.dbp.uripet.auth.domain.QrLoginSession;
import com.dbp.uripet.health.domain.HealthRecord;
import com.dbp.uripet.pet.domain.PetResponsible;
import com.dbp.uripet.user.domain.enums.LanguagePreference;
import com.dbp.uripet.user.domain.enums.ThemePreference;
import com.dbp.uripet.user.domain.enums.UserRole;
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.WorkspaceMember;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String uid;

    @Column(unique = true, nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String phone;

    @Column(nullable = false)
    private boolean verified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_language", nullable = false)
    @Builder.Default
    private LanguagePreference preferredLanguage = LanguagePreference.ES;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_theme", nullable = false)
    @Builder.Default
    private ThemePreference preferredTheme = ThemePreference.SYSTEM;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_code_expires_at")
    private ZonedDateTime verificationCodeExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<PetResponsible> responsibilities = new ArrayList<>();

    @OneToMany(
            mappedBy = "createdBy",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<HealthRecord> healthRecords = new ArrayList<>();

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<QrLoginSession> qrLoginSessions = new ArrayList<>();

    @OneToMany(
            mappedBy = "owner",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<Workspace> ownedWorkspaces = new ArrayList<>();

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<WorkspaceMember> workspaceMemberships = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();

        if (this.uid == null || this.uid.isBlank()) {
            this.uid = "USR-" + UUID.randomUUID().toString().toUpperCase();
        }

        if (this.role == null) {
            this.role = UserRole.USER;
        }

        if (this.preferredLanguage == null) {
            this.preferredLanguage = LanguagePreference.ES;
        }

        if (this.preferredTheme == null) {
            this.preferredTheme = ThemePreference.SYSTEM;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.preferredLanguage == null) {
            this.preferredLanguage = LanguagePreference.ES;
        }

        if (this.preferredTheme == null) {
            this.preferredTheme = ThemePreference.SYSTEM;
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}