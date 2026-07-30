package az.codlab.menu.dto;

import az.codlab.common.type.LocalizedString;
import jakarta.validation.constraints.NotNull;
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
public class CategoryRequest {

    @NotNull
    LocalizedString name;

    String icon;

    Integer sortOrder;

    @NotNull
    UUID orgId;

}
