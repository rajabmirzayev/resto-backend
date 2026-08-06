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
public class UserDto {

    UUID id;
    String keycloakId;
    String name;
    String username;
    String email;
    String phone;
    UUID orgId;
    RoleBriefDto role;
    @JsonProperty("isActive")
    boolean active;

}
