package az.codlab.menu.dto;

import az.codlab.common.type.LocalizedString;
import az.codlab.common.validation.ValidLocalizedString;
import jakarta.validation.constraints.NotNull;
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
public class MenuItemRequest {

    @NotNull
    @ValidLocalizedString(maxLength = 100)
    LocalizedString name;

    @ValidLocalizedString(maxLength = 500)
    LocalizedString description;

    @NotNull
    BigDecimal price;

    @NotNull
    UUID categoryId;

    Integer preparationTime;

    Boolean isAvailable;

    String imageUrl;

    @NotNull
    UUID orgId;

}
