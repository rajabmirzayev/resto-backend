package az.codlab.order.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientTableResponse {
    private UUID id;
    private Integer tableNumber;
    private Integer capacity;
    private String status;
    private UUID sectionId;
    private UUID currentOrderId;
    private Object reservation;
    private UUID orgId;
}
