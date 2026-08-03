package az.flowix.menu.entity;

import az.flowix.common.jpa.entity.SoftDeletableCoreEntity;
import az.flowix.common.type.LocalizedString;
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

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MenuItem extends SoftDeletableCoreEntity {

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "name", columnDefinition = "jsonb", nullable = false)
    LocalizedString name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "description", columnDefinition = "jsonb")
    LocalizedString description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    BigDecimal price;

    @Column(name = "category_id", nullable = false)
    UUID categoryId;

    @Column(name = "image_url")
    String imageUrl;

    @Column(name = "is_available", nullable = false)
    boolean isAvailable;

    @Column(name = "preparation_time")
    Integer preparationTime;

    @Column(name = "org_id", nullable = false)
    UUID orgId;

}
