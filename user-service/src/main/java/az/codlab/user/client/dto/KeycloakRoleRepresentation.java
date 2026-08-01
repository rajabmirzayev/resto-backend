package az.codlab.user.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakRoleRepresentation(
        String id,
        String name,
        String description,
        String containerId
) {
}
