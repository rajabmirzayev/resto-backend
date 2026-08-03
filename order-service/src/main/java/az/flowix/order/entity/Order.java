package az.flowix.order.entity;

import az.flowix.common.enums.OrderSource;
import az.flowix.common.enums.OrderStatus;
import az.flowix.common.enums.PaymentMethod;
import az.flowix.common.enums.PaymentStatus;
import az.flowix.common.jpa.entity.CoreEntity;
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

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order extends CoreEntity {

    @Column(name = "table_id", nullable = false)
    UUID tableId;

    @Column(name = "table_number")
    Integer tableNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    PaymentStatus paymentStatus;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    BigDecimal totalAmount;

    @Column(name = "waiter_id")
    UUID waiterId;

    @Column(name = "waiter_name")
    String waiterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_source", nullable = false, length = 20)
    OrderSource orderSource;

    @Column(name = "waiter_confirmed", nullable = false)
    boolean waiterConfirmed;

    @Column(name = "confirmed_by")
    String confirmedBy;

    @Column(name = "customer_photo")
    String customerPhoto;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 10)
    PaymentMethod paymentMethod;

    @Column(name = "payment_requested", nullable = false)
    boolean paymentRequested;

    @Column(name = "cancel_reason")
    String cancelReason;

    @Column(name = "org_id", nullable = false)
    UUID orgId;

}
