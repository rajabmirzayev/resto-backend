package az.codlab.customer.client.dto;

import az.codlab.common.type.TableReservation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TableServiceTableResponse {
    UUID id;
    Integer tableNumber;
    Integer capacity;
    String status;
    UUID sectionId;
    UUID currentOrderId;
    TableReservation reservation;
    UUID orgId;
}
