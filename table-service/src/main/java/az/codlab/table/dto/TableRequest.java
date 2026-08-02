package az.codlab.table.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
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
public class TableRequest {

    @NotNull
    @Positive
    @Max(9999)
    Integer tableNumber;

    @NotNull
    @Positive
    @Max(500)
    Integer capacity;

    UUID sectionId;

    @NotNull
    UUID orgId;

}
