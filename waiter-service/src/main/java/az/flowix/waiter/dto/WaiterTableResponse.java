package az.flowix.waiter.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WaiterTableResponse {

    UUID id;
    Integer tableNumber;
    Integer capacity;
    String status;
    String section;
    UUID currentOrderId;
    OrderSummary orderSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OrderSummary {
        BigDecimal totalAmount;
        int itemCount;
        String status;
    }

}
