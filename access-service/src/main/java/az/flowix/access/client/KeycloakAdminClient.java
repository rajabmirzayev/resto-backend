package az.flowix.access.client;

import az.flowix.access.client.dto.KeycloakTokenResponse;
import az.flowix.access.client.dto.KeycloakUserRepresentation;
import az.flowix.access.config.KeycloakProperties;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Thin client over the Keycloak Admin REST API. Authenticates with the
 * {@code resto-auth} client service account (client_credentials grant) whose
 * service account user must hold the {@code manage-users} realm-management role.
 */
@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final RestClient restClient;
    private final KeycloakProperties props;

    private volatile CachedToken cachedToken;

    public KeycloakAdminClient(RestClient keycloakRestClient, KeycloakProperties props) {
        this.restClient = keycloakRestClient;
        this.props = props;
    }

    public List<KeycloakUserRepresentation> findUsersByUsername(String username) {
        try {
            return restClient.get()
                    .uri(props.getAdminBaseUri() + "/users?username={username}&exact={exact}", username, true)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<KeycloakUserRepresentation>>() {
                    });
        } catch (RestClientResponseException ex) {
            throw keycloakFailure("find user '" + username + "'", ex);
        }
    }

    public List<KeycloakUserRepresentation> findUsersByEmail(String email) {
        try {
            return restClient.get()
                    .uri(props.getAdminBaseUri() + "/users?email={email}&exact={exact}", email, true)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<KeycloakUserRepresentation>>() {
                    });
        } catch (RestClientResponseException ex) {
            throw keycloakFailure("find user by email '" + email + "'", ex);
        }
    }

    public boolean userExistsByUsername(String username) {
        return !findUsersByUsername(username).isEmpty();
    }

    public boolean userExistsByEmail(String email) {
        return !findUsersByEmail(email).isEmpty();
    }

    public String createUser(NewUser user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("username", user.username());
        body.put("email", user.email());
        body.put("firstName", user.firstName());
        body.put("enabled", true);
        body.put("emailVerified", true);
        body.put("requiredActions", List.of());
        body.put("attributes", Map.of("organizationId", List.of(user.orgId().toString())));

        try {
            var response = restClient.post()
                    .uri(props.getAdminBaseUri() + "/users")
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            String userId = userIdFromLocation(response.getHeaders().getLocation() != null
                    ? response.getHeaders().getLocation().getPath() : null);
            if (userId == null) {
                throw new KeycloakException("Keycloak did not return a user id for '" + user.username() + "'");
            }
            log.debug("Keycloak user created: {} ({})", user.username(), userId);
            return userId;
        } catch (RestClientResponseException ex) {
            throw keycloakFailure("create user '" + user.username() + "'", ex);
        }
    }

    public void resetPassword(String userId, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "password");
        body.put("value", password);
        body.put("temporary", false);

        try {
            restClient.put()
                    .uri(props.getAdminBaseUri() + "/users/{userId}/reset-password", userId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Keycloak password updated for user {}", userId);
        } catch (RestClientResponseException ex) {
            throw keycloakFailure("set password for user '" + userId + "'", ex);
        }
    }

    public void updateUser(String userId, Map<String, Object> fields) {
        try {
            KeycloakUserRepresentation current = getUser(userId);
            Map<String, Object> merged = new LinkedHashMap<>();
            if (current != null) {
                if (current.username() != null) {
                    merged.put("username", current.username());
                }
                if (current.email() != null) {
                    merged.put("email", current.email());
                }
                if (current.firstName() != null) {
                    merged.put("firstName", current.firstName());
                }
                if (current.lastName() != null) {
                    merged.put("lastName", current.lastName());
                }
                if (current.enabled() != null) {
                    merged.put("enabled", current.enabled());
                }
                if (current.emailVerified() != null) {
                    merged.put("emailVerified", current.emailVerified());
                }
                if (current.attributes() != null) {
                    merged.put("attributes", new LinkedHashMap<>(current.attributes()));
                }
            }

            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                if ("attributes".equals(entry.getKey())) {
                    @SuppressWarnings("unchecked")
                    Map<String, List<String>> incoming = (Map<String, List<String>>) entry.getValue();
                    @SuppressWarnings("unchecked")
                    Map<String, List<String>> existing = (Map<String, List<String>>) merged.get("attributes");
                    if (existing == null) {
                        existing = new LinkedHashMap<>();
                    }
                    existing.putAll(incoming);
                    merged.put("attributes", existing);
                } else {
                    merged.put(entry.getKey(), entry.getValue());
                }
            }

            restClient.put()
                    .uri(props.getAdminBaseUri() + "/users/{userId}", userId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(merged)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Keycloak user updated: {}", userId);
        } catch (RestClientResponseException ex) {
            throw keycloakFailure("update user '" + userId + "'", ex);
        }
    }

    public KeycloakUserRepresentation getUser(String userId) {
        try {
            return restClient.get()
                    .uri(props.getAdminBaseUri() + "/users/{userId}", userId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken()))
                    .retrieve()
                    .body(KeycloakUserRepresentation.class);
        } catch (RestClientResponseException ex) {
            throw keycloakFailure("get user '" + userId + "'", ex);
        }
    }

    public void setAttributes(String userId, Map<String, List<String>> attributes) {
        updateUser(userId, Map.of("attributes", attributes));
    }

    public void setEnabled(String userId, boolean enabled) {
        updateUser(userId, Map.of("enabled", enabled));
    }

    public void deleteUser(String userId) {
        try {
            restClient.delete()
                    .uri(props.getAdminBaseUri() + "/users/{userId}", userId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken()))
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Keycloak user deleted: {}", userId);
        } catch (RestClientResponseException ex) {
            throw keycloakFailure("delete user '" + userId + "'", ex);
        }
    }

    private synchronized String accessToken() {
        if (cachedToken != null && cachedToken.expiresAt().isAfter(Instant.now())) {
            return cachedToken.accessToken();
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());

        try {
            var response = restClient.post()
                    .uri(props.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new KeycloakException("Keycloak returned no service account token");
            }
            cachedToken = new CachedToken(response.accessToken(),
                    Instant.now().plusSeconds(Math.max(1, response.expiresIn() - 30)));
            log.debug("Keycloak service account token acquired (expires in {}s)", response.expiresIn());
            return response.accessToken();
        } catch (RestClientResponseException ex) {
            throw keycloakFailure("acquire service account token", ex);
        }
    }

    private String userIdFromLocation(String location) {
        if (location == null) {
            return null;
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private KeycloakException keycloakFailure(String operation, RestClientResponseException ex) {
        log.error("Keycloak failed to {}: {} — {}", operation, ex.getStatusCode(), ex.getResponseBodyAsString());
        return new KeycloakException("Keycloak failed to " + operation + ": " + ex.getStatusCode(), ex);
    }

    public record NewUser(String username, String email, String firstName, UUID orgId) {
    }

    private record CachedToken(String accessToken, Instant expiresAt) {
    }
}
