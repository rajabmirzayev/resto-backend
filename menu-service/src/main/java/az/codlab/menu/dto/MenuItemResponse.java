package az.codlab.menu.dto;

import az.codlab.common.type.LocalizedString;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MenuItemResponse {

    UUID id;
    LocalizedString name;
    LocalizedString description;
    BigDecimal price;
    UUID categoryId;
    String imageUrl;
    @JsonProperty("isAvailable")
    boolean isAvailable;
    Integer preparationTime;
    UUID orgId;
    Instant createdAt;

}
