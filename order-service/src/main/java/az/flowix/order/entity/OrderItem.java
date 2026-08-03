package az.flowix.order.entity;

import az.flowix.common.jpa.entity.CoreEntity;
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

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItem extends CoreEntity {

    @Column(name = "order_id", nullable = false)
    UUID orderId;

    @Column(name = "menu_item_id", nullable = false)
    UUID menuItemId;

    @Column(name = "menu_item_name", nullable = false)
    String menuItemName;

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    BigDecimal price;

    @Column(name = "notes")
    String notes;

    @Column(name = "status", nullable = false, length = 20)
    String status;

    @Column(name = "org_id", nullable = false)
    UUID orgId;

}
