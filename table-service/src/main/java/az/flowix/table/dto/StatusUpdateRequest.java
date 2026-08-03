package az.flowix.table.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatusUpdateRequest {

    @NotBlank
    @Pattern(regexp = "(?i)^(AVAILABLE|OCCUPIED|RESERVED|CLEANING)$",
            message = "status must be one of AVAILABLE, OCCUPIED, RESERVED, CLEANING")
    String status;

    UUID currentOrderId;

}
