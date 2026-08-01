package az.codlab.menu.dto;

import az.codlab.common.type.LocalizedString;
import az.codlab.common.validation.ValidLocalizedString;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
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
public class MenuItemUpdateRequest {

    @ValidLocalizedString(maxLength = 100)
    LocalizedString name;

    @ValidLocalizedString(maxLength = 500)
    LocalizedString description;

    @Positive
    @Digits(integer = 8, fraction = 2)
    BigDecimal price;

    UUID categoryId;

    @PositiveOrZero
    @Max(10080)
    Integer preparationTime;

    Boolean isAvailable;

    @Size(max = 512)
    @Pattern(regexp = "^(https?://|/)[^\\p{Cc}]*$",
            message = "imageUrl must be a valid http(s) URL or a relative path")
    String imageUrl;

}
