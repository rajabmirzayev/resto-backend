package az.flowix.auth.client;

import az.flowix.auth.config.KeycloakProperties;
import az.flowix.auth.dto.keycloak.KeycloakTokenResponse;
import az.flowix.auth.error.AuthErrorCode;
import az.flowix.common.exception.handling.exception.BaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class KeycloakClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakClient.class);

    private final RestClient restClient;
    private final KeycloakProperties props;

    public KeycloakClient(RestClient keycloakRestClient, KeycloakProperties props) {
        this.restClient = keycloakRestClient;
        this.props = props;
    }

    public KeycloakTokenResponse login(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("username", username);
        form.add("password", password);

        return postTokenRequest(form, "login");
    }

    public KeycloakTokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("refresh_token", refreshToken);

        return postTokenRequest(form, "refresh");
    }

    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", props.getClientId());
        form.add("client_secret", props.getClientSecret());
        form.add("refresh_token", refreshToken);

        try {
            restClient.post()
                    .uri(props.getLogoutUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Successfully revoked refresh token");
        } catch (RestClientResponseException ex) {
            log.error("Keycloak logout failed: {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw AuthErrorCode.LOGOUT_FAILED.exception();
        } catch (Exception ex) {
            log.error("Keycloak logout error", ex);
            throw AuthErrorCode.KEYCLOAK_UNAVAILABLE.exception();
        }
    }

    private KeycloakTokenResponse postTokenRequest(MultiValueMap<String, String> form, String operation) {
        try {
            var response = restClient.post()
                    .uri(props.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

            if (response == null || response.accessToken() == null) {
                log.error("Keycloak {} returned empty response", operation);
                throw AuthErrorCode.KEYCLOAK_UNAVAILABLE.exception();
            }

            log.debug("Keycloak {} succeeded", operation);
            return response;
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            var body = ex.getResponseBodyAsString();
            log.warn("Keycloak {} failed: HTTP {} — {}", operation, status, body);

            if (status == 401 || status == 400) {
                throw AuthErrorCode.INVALID_CREDENTIALS.exception();
            }
            throw AuthErrorCode.KEYCLOAK_UNAVAILABLE.exception();
        } catch (BaseException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error during Keycloak {}", operation, ex);
            throw AuthErrorCode.KEYCLOAK_UNAVAILABLE.exception();
        }
    }
}
