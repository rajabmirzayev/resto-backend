package az.flowix.access.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionDto {

    UUID id;
    String code;
    String name;
    String description;
    ModuleRefDto module;
    UiGroupRefDto uiGroup;
    int sortOrder;
    @JsonProperty("isActive")
    boolean active;

}
