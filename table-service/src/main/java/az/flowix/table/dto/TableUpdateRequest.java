package az.flowix.table.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
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
public class TableUpdateRequest {

    @Positive
    @Max(9999)
    Integer tableNumber;

    @Positive
    @Max(500)
    Integer capacity;

    UUID sectionId;

    @Pattern(regexp = "(?i)^(AVAILABLE|OCCUPIED|RESERVED|CLEANING)$",
            message = "status must be one of AVAILABLE, OCCUPIED, RESERVED, CLEANING")
    String status;

}
