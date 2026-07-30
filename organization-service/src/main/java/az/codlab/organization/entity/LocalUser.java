package az.codlab.organization.entity;

import az.codlab.common.jpa.entity.SoftDeletableCoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

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
public class LocalUser extends SoftDeletableCoreEntity {

    @Column(name = "keycloak_id", unique = true)
    String keycloakId;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "username", nullable = false)
    String username;

    @Column(name = "email")
    String email;

    @Column(name = "password", nullable = false)
    String password;

    @Column(name = "role", nullable = false, length = 20)
    String role;

    @Column(name = "role_id")
    UUID roleId;

    @Column(name = "org_id")
    UUID orgId;

    @Column(name = "avatar")
    String avatar;

    @Column(name = "is_active", nullable = false)
    boolean isActive;

}
