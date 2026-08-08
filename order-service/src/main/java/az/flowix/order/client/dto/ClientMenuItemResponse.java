package az.flowix.order.client.dto;

import az.flowix.common.type.LocalizedString;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientMenuItemResponse {
    private UUID id;
    private LocalizedString name;
    private LocalizedString description;
    private BigDecimal price;
    private UUID categoryId;
    private String imageUrl;

    @JsonProperty("isAvailable")
    @JsonAlias("available")
    private boolean isAvailable;

    private Integer preparationTime;
    private UUID orgId;
}
