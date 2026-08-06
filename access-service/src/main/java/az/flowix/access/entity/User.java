package az.flowix.access.entity;

import az.flowix.common.jpa.entity.SoftDeletableCoreEntity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends SoftDeletableCoreEntity {

    @Column(name = "keycloak_id", unique = true)
    String keycloakId;

    @Column(name = "password")
    String password;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "username", nullable = false)
    String username;

    @Column(name = "email")
    String email;

    @Column(name = "phone")
    String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    Role role;

    @Column(name = "org_id")
    UUID orgId;

    @Column(name = "avatar", length = 512)
    String avatar;

    @Column(name = "is_active", nullable = false)
    boolean isActive;

    public boolean getActive() {
        return isActive;
    }

}
