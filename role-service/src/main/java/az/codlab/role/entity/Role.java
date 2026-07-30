package az.codlab.role.entity;

import az.codlab.common.jpa.entity.SoftDeletableCoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Role extends SoftDeletableCoreEntity {

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "permissions", columnDefinition = "JSONB", nullable = false)
    List<String> permissions;

    @Column(name = "is_system", nullable = false)
    boolean isSystem;

    @Column(name = "org_id")
    UUID orgId;

}
