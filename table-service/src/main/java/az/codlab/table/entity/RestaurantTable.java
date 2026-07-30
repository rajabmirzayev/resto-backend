package az.codlab.table.entity;

import az.codlab.common.enums.TableStatus;
import az.codlab.common.jpa.entity.SoftDeletableCoreEntity;
import az.codlab.common.type.TableReservation;
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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "restaurant_tables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RestaurantTable extends SoftDeletableCoreEntity {

    @Column(name = "table_number", nullable = false)
    Integer tableNumber;

    @Column(name = "capacity", nullable = false)
    Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    TableStatus status;

    @Column(name = "section_id")
    UUID sectionId;

    @Column(name = "current_order_id")
    UUID currentOrderId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reservation", columnDefinition = "jsonb")
    TableReservation reservation;

    @Column(name = "org_id", nullable = false)
    UUID orgId;

}
