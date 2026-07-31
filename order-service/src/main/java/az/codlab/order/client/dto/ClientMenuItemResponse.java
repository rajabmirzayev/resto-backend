package az.codlab.order.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientMenuItemResponse {
    private UUID id;
    private Object name;
    private Object description;
    private BigDecimal price;
    private UUID categoryId;
    private String imageUrl;
    private boolean isAvailable;
    private Integer preparationTime;
    private UUID orgId;
}
