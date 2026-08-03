package az.flowix.organization.entity;

import az.flowix.common.jpa.entity.SoftDeletableCoreEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LocalRole extends SoftDeletableCoreEntity {

    @Column(name = "name", nullable = false)
    String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permissions", columnDefinition = "jsonb", nullable = false)
    List<String> permissions;

    @Column(name = "is_system", nullable = false)
    boolean isSystem;

    @Column(name = "org_id")
    UUID orgId;

}
