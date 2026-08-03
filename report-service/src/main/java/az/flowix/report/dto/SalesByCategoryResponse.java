package az.flowix.report.dto;

import az.flowix.common.type.LocalizedString;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesByCategoryResponse {
    UUID categoryId;
    LocalizedString name;
    int count;
}
