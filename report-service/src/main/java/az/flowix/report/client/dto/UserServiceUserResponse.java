package az.flowix.report.client.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserServiceUserResponse {
    UUID id;
    String name;
    RoleBrief role;
    UUID orgId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class RoleBrief {
        String code;
    }

    public String getRole() {
        return role != null ? role.getCode() : null;
    }
}
