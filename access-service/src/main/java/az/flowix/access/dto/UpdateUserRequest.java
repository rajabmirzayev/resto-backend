package az.flowix.access.dto;

import az.flowix.common.validation.ValidPhone;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
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
public class UpdateUserRequest {

    String name;

    String username;

    @Email
    String email;

    @Size(min = 6)
    String password;

    @ValidPhone
    String phone;

    Boolean isActive;

}
