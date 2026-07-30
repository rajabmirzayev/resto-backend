package az.codlab.organization.dto;

import java.time.Instant;
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
public class OrganizationDto {

    UUID id;
    String name;
    String slug;
    String adminName;
    String adminEmail;
    String logoUrl;
    String phone;
    String address;
    Instant createdAt;

}
