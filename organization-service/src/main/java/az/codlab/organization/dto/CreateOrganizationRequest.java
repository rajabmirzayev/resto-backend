package az.codlab.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class CreateOrganizationRequest {

    @NotBlank(message = "Organization name is required")
    String name;

    @NotBlank(message = "Admin name is required")
    String adminName;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Invalid email format")
    String adminEmail;

    @NotBlank(message = "Admin password is required")
    String adminPassword;

}
