package az.codlab.user.dto;

import az.codlab.common.validation.ValidPhone;

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
public class UpdateUserRequest {

    String name;

    String username;

    String password;

    UUID roleId;

    @ValidPhone
    String phone;

    Boolean isActive;

}
