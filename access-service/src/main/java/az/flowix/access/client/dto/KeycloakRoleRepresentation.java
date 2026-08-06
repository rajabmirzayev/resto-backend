package az.flowix.access.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakRoleRepresentation(
        String id,
        String name,
        String description,
        String containerId
) {
}
