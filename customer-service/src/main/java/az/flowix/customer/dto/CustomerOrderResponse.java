package az.flowix.customer.dto;

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
public class CustomerOrderResponse {

    String id;
    List<ItemResponse> items;
    UUID tableId;
    Integer tableNumber;
    String status;
    String paymentStatus;
    BigDecimal totalAmount;
    String orderSource;
    boolean waiterConfirmed;
    String customerPhoto;
    String paymentMethod;
    boolean paymentRequested;
    UUID orgId;
    Instant createdAt;
    Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ItemResponse {
        String id;
        UUID menuItemId;
        String menuItemName;
        Integer quantity;
        BigDecimal price;
        String notes;
        String status;
    }

}
