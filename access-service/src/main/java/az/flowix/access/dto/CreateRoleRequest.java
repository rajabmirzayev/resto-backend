package az.flowix.access.dto;

import az.flowix.common.enums.UiScope;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateRoleRequest {

    @NotBlank
    String code;

    @NotBlank
    String name;

    @NotNull
    UiScope uiScope;

    UUID orgId;

    List<UUID> permissionIds;

}
