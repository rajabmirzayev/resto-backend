package az.codlab.organization.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientOrderResponse {
    private String id;
    private UUID tableId;
    private String status;
    private String paymentStatus;
    private UUID orgId;
}
