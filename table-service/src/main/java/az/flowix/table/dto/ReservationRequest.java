package az.flowix.table.dto;

import az.flowix.common.validation.ValidPhone;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReservationRequest {

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "^[^\\p{Cc}]*$",
            message = "guestName must not contain control characters")
    String guestName;

    @NotBlank
    @ValidPhone
    String phone;

    @NotNull
    @Future
    Instant time;

    @NotNull
    @Positive
    @Max(100)
    Integer guestCount;

    @Size(max = 500)
    @Pattern(regexp = "^[^\\p{Cc}]*$",
            message = "notes must not contain control characters")
    String notes;

}
