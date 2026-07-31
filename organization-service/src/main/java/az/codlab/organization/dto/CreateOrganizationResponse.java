package az.codlab.organization.dto;

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
public class CreateOrganizationResponse {
    OrganizationDto organization;
    UserDto adminUser;
    RoleDto adminRole;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserDto {
        UUID id;
        String name;
        String username;
        String email;
        String role;
        UUID roleId;
        UUID orgId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoleDto {
        UUID id;
        String name;
        List<String> permissions;
        Boolean isSystem;
        UUID orgId;
    }

}
