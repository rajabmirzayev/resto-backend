package az.flowix.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserResponse {

    UUID id;
    String keycloakId;
    String name;
    String username;
    String email;
    String phone;
    String role;
    UUID roleId;
    UUID orgId;
    String avatar;
    boolean isActive;
    Instant createdAt;

}
