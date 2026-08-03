package az.flowix.user.dto;

import az.flowix.common.validation.ValidPhone;
import az.flowix.user.entity.UserRole;
import jakarta.validation.constraints.NotBlank;
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
public class CreateUserRequest {

    @NotBlank
    String name;

    @NotBlank
    String username;

    @NotBlank
    String password;

    @NotNull
    UUID roleId;

    @NotNull
    UUID orgId;

    String email;

    @ValidPhone
    String phone;

    UserRole role;

}
