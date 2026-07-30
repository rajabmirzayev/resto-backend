package az.codlab.menu.dto;

import az.codlab.common.type.LocalizedString;
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

    LocalizedString name;

    LocalizedString description;

    BigDecimal price;

    UUID categoryId;

    Integer preparationTime;

    Boolean isAvailable;

    String imageUrl;

}
