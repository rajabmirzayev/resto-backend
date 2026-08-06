package az.flowix.access.dto;

import az.flowix.common.enums.UiScope;

import java.util.List;
import java.util.UUID;

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
public class RoleResponse {

    UUID id;
    String code;
    String name;
    UiScope uiScope;
    Boolean isSystem;
    Boolean isActive;
    UUID orgId;
    List<UUID> permissionIds;
    List<PermissionDto> permissions;

}
