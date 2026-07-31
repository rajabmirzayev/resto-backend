package az.codlab.setting.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientOrganizationDto {
    private UUID id;
    private String name;
    private String slug;
    private String adminName;
    private String adminEmail;
    private String logoUrl;
    private String phone;
    private String address;
    private Instant createdAt;
}
