package az.codlab.kitchen.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KitchenOrderResponse {

    String id;
    List<KitchenItemResponse> items;
    UUID tableId;
    Integer tableNumber;
    String status;
    String paymentStatus;
    BigDecimal totalAmount;
    String waiterName;
    String orderSource;
    Instant createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class KitchenItemResponse {

        String id;
        UUID menuItemId;
        String menuItemName;
        Integer quantity;
        BigDecimal price;
        String notes;
        String status;

    }

}
