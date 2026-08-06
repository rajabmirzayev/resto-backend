package az.flowix.access.client.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakUserRepresentation(
        String id,
        String username,
        String email,
        Boolean enabled,
        String firstName,
        String lastName,
        Boolean emailVerified,
        Map<String, List<String>> attributes
) {
}
