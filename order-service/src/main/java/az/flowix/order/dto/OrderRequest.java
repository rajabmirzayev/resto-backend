package az.flowix.order.dto;

import az.flowix.common.enums.OrderSource;
import az.flowix.common.exception.handling.validation.ValidEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class OrderRequest {

    @NotNull(message = "Organization ID is required")
    UUID orgId;

    @NotNull(message = "Table ID is required")
    UUID tableId;

    UUID waiterId;

    String waiterName;

    @NotBlank(message = "Order source is required")
    @ValidEnum(enumClass = OrderSource.class, message = "Invalid order source. Allowed: WAITER, CUSTOMER")
    String orderSource;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    List<OrderItemRequest> items;

    String customerPhoto;

    String paymentMethod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OrderItemRequest {

        @NotNull(message = "Menu item ID is required")
        UUID menuItemId;

        @NotBlank(message = "Menu item name is required")
        String menuItemName;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity;

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.00", message = "Price must not be negative")
        BigDecimal price;

        String notes;

    }

}
