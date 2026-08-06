package az.flowix.access.entity;

import az.flowix.common.jpa.entity.SoftDeletableCoreEntity;

import java.util.UUID;

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

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Permission extends SoftDeletableCoreEntity {

    @Column(name = "code", nullable = false, unique = true)
    String code;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "description")
    String description;

    @Column(name = "module_id", nullable = false)
    UUID moduleId;

    @Column(name = "ui_group_id")
    UUID uiGroupId;

    @Column(name = "sort_order", nullable = false)
    int sortOrder;

    @Column(name = "is_active", nullable = false)
    boolean isActive;

}
