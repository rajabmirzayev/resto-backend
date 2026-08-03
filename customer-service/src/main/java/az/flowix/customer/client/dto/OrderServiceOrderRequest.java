package az.flowix.customer.client.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderServiceOrderRequest {
    UUID orgId;
    UUID tableId;
    UUID waiterId;
    String waiterName;
    String orderSource;
    List<OrderItemRequest> items;
    String customerPhoto;
    String paymentMethod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OrderItemRequest {
        UUID menuItemId;
        String menuItemName;
        Integer quantity;
        BigDecimal price;
        String notes;
    }
}
