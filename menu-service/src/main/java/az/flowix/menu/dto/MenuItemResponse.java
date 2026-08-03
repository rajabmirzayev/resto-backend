package az.flowix.menu.dto;

import az.flowix.common.type.LocalizedString;
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
    boolean available;
    Integer preparationTime;
    UUID orgId;
    Instant createdAt;

    @JsonProperty("isAvailable")
    public boolean isAvailable() {
        return available;
    }

}
