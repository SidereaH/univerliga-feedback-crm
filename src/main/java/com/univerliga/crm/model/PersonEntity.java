package com.univerliga.crm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "persons")
@Getter
@Setter
public class PersonEntity {

    @Id
    private String id;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(nullable = false, length = 320, unique = true)
    private String email;

    @Column(name = "department_id", nullable = false, length = 64)
    private String departmentId;

    @Column(name = "team_id", nullable = false, length = 64)
    private String teamId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PersonRole role;

    @Column(nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_status", nullable = false, length = 32)
    private IdentityStatus identityStatus;

    @Column(name = "keycloak_user_id", length = 128)
    private String keycloakUserId;

    @Column(name = "last_identity_error", length = 2000)
    private String lastIdentityError;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
