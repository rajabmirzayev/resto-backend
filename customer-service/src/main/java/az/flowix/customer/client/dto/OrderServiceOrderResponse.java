package az.flowix.customer.client.dto;

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
public class OrderServiceOrderResponse {
    String id;
    UUID tableId;
    Integer tableNumber;
    List<Item> items;
    String status;
    String paymentStatus;
    BigDecimal totalAmount;
    UUID waiterId;
    String waiterName;
    String orderSource;
    boolean waiterConfirmed;
    String confirmedBy;
    String customerPhoto;
    String paymentMethod;
    boolean paymentRequested;
    String cancelReason;
    String accessToken;
    UUID orgId;
    Instant createdAt;
    Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Item {
        String id;
        UUID menuItemId;
        String menuItemName;
        Integer quantity;
        BigDecimal price;
        String notes;
        String status;
    }
}
