package az.flowix.menu.dto;

import az.flowix.common.type.LocalizedString;
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
public class CategoryResponse {

    UUID id;
    LocalizedString name;
    String icon;
    Integer sortOrder;
    UUID orgId;

}
