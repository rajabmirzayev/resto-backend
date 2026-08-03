package az.flowix.customer.client.dto;

import az.flowix.common.type.LocalizedString;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class MenuServiceItemResponse {
    UUID id;
    LocalizedString name;
    LocalizedString description;
    BigDecimal price;
    UUID categoryId;
    String imageUrl;

    @JsonProperty("isAvailable")
    @JsonAlias("available")
    boolean isAvailable;

    Integer preparationTime;
    UUID orgId;
}
