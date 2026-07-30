package az.codlab.menu.entity;

import az.codlab.common.jpa.entity.SoftDeletableCoreEntity;
import az.codlab.common.type.LocalizedString;
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

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "menu_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MenuCategory extends SoftDeletableCoreEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "name", columnDefinition = "jsonb", nullable = false)
    LocalizedString name;

    @Column(name = "icon", length = 50)
    String icon;

    @Column(name = "sort_order")
    Integer sortOrder;

    @Column(name = "org_id", nullable = false)
    UUID orgId;

}
