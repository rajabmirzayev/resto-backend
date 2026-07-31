package az.codlab.organization.client.dto;

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
public class UserServiceUserResponse {
    UUID id;
    String name;
    String username;
    String email;
    String role;
    UUID roleId;
    UUID orgId;
    boolean isActive;
}
