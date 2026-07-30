package az.codlab.customer.dto;

import jakarta.validation.Valid;
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
public class CustomerOrderRequest {

    @NotNull
    UUID orgId;

    @NotNull
    UUID tableId;

    @NotEmpty
    @Valid
    List<ItemRequest> items;

    String customerPhoto;

    String paymentMethod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ItemRequest {

        @NotNull
        UUID menuItemId;

        @NotNull
        String menuItemName;

        @NotNull
        Integer quantity;

        @NotNull
        BigDecimal price;

        String notes;

    }

}
