package az.codlab.user.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakUserRepresentation(
        String id,
        String username,
        String email,
        Boolean enabled,
        String firstName,
        String lastName,
        Boolean emailVerified
) {
}
