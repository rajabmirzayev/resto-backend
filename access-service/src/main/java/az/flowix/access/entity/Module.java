package az.flowix.access.entity;

import az.flowix.common.jpa.entity.SoftDeletableCoreEntity;

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
@Table(name = "modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Module extends SoftDeletableCoreEntity {

    @Column(name = "code", nullable = false, length = 64)
    String code;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "sort_order", nullable = false)
    int sortOrder;

    @Column(name = "is_active", nullable = false)
    boolean isActive;

}
