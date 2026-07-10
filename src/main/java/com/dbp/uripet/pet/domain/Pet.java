package com.dbp.uripet.pet.domain;

import com.dbp.uripet.health.domain.HealthRecord;
import com.dbp.uripet.pet.domain.enums.QrCornerStyle;
import com.dbp.uripet.pet.domain.enums.QrFrameStyle;
import com.dbp.uripet.pet.domain.enums.QrPatternStyle;
import com.dbp.uripet.workspace.domain.Workspace;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            unique = true,
            nullable = false,
            updatable = false
    )
    private String pid;

    private String name;

    private String species;

    private String breed;

    private LocalDate birthDate;

    private Double weight;

    private String color;

    @Column(
            unique = true,
            nullable = false,
            updatable = false
    )
    private UUID qrCode;

    private String emergencyContact;

    @ElementCollection(fetch = FetchType.EAGER)
    @Builder.Default
    private List<String> imagesUrl = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "workspace_id",
            nullable = false
    )
    private Workspace workspace;

    /*
     * Configuración de privacidad pública.
     */

    @Column(
            name = "show_emergency_contact",
            nullable = false
    )
    @Builder.Default
    private boolean showEmergencyContact = true;

    @Column(
            name = "show_basic_information",
            nullable = false
    )
    @Builder.Default
    private boolean showBasicInformation = true;

    @Column(
            name = "show_health_summary",
            nullable = false
    )
    @Builder.Default
    private boolean showHealthSummary = false;

    /*
     * Configuración visual del código QR.
     */

    @Column(
            name = "qr_foreground_color",
            nullable = false,
            length = 7
    )
    @Builder.Default
    private String qrForegroundColor = "#000000";

    @Column(
            name = "qr_background_color",
            nullable = false,
            length = 7
    )
    @Builder.Default
    private String qrBackgroundColor = "#FFFFFF";

    @Enumerated(EnumType.STRING)
    @Column(
            name = "qr_pattern_style",
            nullable = false
    )
    @Builder.Default
    private QrPatternStyle qrPatternStyle =
            QrPatternStyle.SQUARE;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "qr_corner_style",
            nullable = false
    )
    @Builder.Default
    private QrCornerStyle qrCornerStyle =
            QrCornerStyle.SQUARE;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "qr_frame_style",
            nullable = false
    )
    @Builder.Default
    private QrFrameStyle qrFrameStyle =
            QrFrameStyle.NONE;

    @Column(
            name = "qr_show_pet_image",
            nullable = false
    )
    @Builder.Default
    private boolean qrShowPetImage = false;

    @Column(
            name = "qr_show_label",
            nullable = false
    )
    @Builder.Default
    private boolean qrShowLabel = true;

    @Column(
            name = "qr_label_text",
            length = 40
    )
    private String qrLabelText;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private ZonedDateTime createdAt;

    @OneToMany(
            mappedBy = "pet",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<PetResponsible> responsibilities =
            new ArrayList<>();

    @OneToMany(
            mappedBy = "pet",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<HealthRecord> healthRecords =
            new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now();

        if (this.pid == null || this.pid.isBlank()) {
            this.pid = "PET-"
                    + UUID.randomUUID()
                    .toString()
                    .substring(0, 7)
                    .toUpperCase();
        }

        if (this.qrCode == null) {
            this.qrCode = UUID.randomUUID();
        }

        applyDefaultPrivacySettings();
        applyDefaultQrSettings();
    }

    @PreUpdate
    protected void onUpdate() {
        applyDefaultQrSettings();
    }

    private void applyDefaultPrivacySettings() {
        /*
         * Los boolean primitivos ya tienen valores seguros
         * definidos por defecto.
         */
    }

    private void applyDefaultQrSettings() {
        if (this.qrForegroundColor == null
                || this.qrForegroundColor.isBlank()) {

            this.qrForegroundColor = "#000000";
        }

        if (this.qrBackgroundColor == null
                || this.qrBackgroundColor.isBlank()) {

            this.qrBackgroundColor = "#FFFFFF";
        }

        if (this.qrPatternStyle == null) {
            this.qrPatternStyle = QrPatternStyle.SQUARE;
        }

        if (this.qrCornerStyle == null) {
            this.qrCornerStyle = QrCornerStyle.SQUARE;
        }

        if (this.qrFrameStyle == null) {
            this.qrFrameStyle = QrFrameStyle.NONE;
        }

        if (this.qrShowLabel
                && (this.qrLabelText == null
                || this.qrLabelText.isBlank())) {

            this.qrLabelText = this.name != null
                    && !this.name.isBlank()
                    ? "Encontraste a " + this.name
                    : "Mascota encontrada";
        }
    }
}