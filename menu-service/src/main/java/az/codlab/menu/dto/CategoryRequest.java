package az.codlab.menu.dto;

import az.codlab.common.type.LocalizedString;
import az.codlab.common.validation.ValidLocalizedString;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
public class CategoryRequest {

    @NotNull
    @ValidLocalizedString(maxLength = 100)
    LocalizedString name;

    @Size(max = 50)
    @Pattern(regexp = "^[^\\p{Cc}]*$", message = "icon must not contain control characters")
    String icon;

    @PositiveOrZero
    @Max(10000)
    Integer sortOrder;

    @NotNull
    UUID orgId;

}
