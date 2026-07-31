package az.codlab.user.entity;

import az.codlab.common.jpa.entity.SoftDeletableCoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    UserRole role;

    @Column(name = "role_id")
    UUID roleId;

    @Column(name = "org_id")
    UUID orgId;

    @Column(name = "avatar", length = 512)
    String avatar;

    @Column(name = "is_active", nullable = false)
    boolean isActive;

}
