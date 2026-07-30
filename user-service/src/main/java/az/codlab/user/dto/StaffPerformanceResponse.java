package az.codlab.user.dto;

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
public class StaffPerformanceResponse {

    UUID userId;
    String name;
    String role;
    long totalOrders;
    long completedOrders;
    BigDecimal revenue;
    long activeOrders;

}
