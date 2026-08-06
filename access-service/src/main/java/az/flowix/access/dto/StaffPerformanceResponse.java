package az.flowix.access.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffPerformanceResponse {

    UUID userId;
    String name;
    String role;
    long totalOrders;
    long completedOrders;
    BigDecimal revenue;
    long activeOrders;

}
