package az.codlab.customer.dto;

import az.codlab.common.type.LocalizedString;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CustomerMenuResponse {

    List<CategoryResponse> categories;
    List<ItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CategoryResponse {
        UUID id;
        LocalizedString name;
        String icon;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ItemResponse {
        UUID id;
        LocalizedString name;
        LocalizedString description;
        BigDecimal price;
        UUID categoryId;
        String imageUrl;
        boolean isAvailable;
        Integer preparationTime;
    }

}
