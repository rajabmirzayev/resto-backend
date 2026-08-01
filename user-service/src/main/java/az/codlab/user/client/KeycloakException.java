package az.codlab.user.client;

/**
 * Thrown when an interaction with the Keycloak Admin REST API fails.
 */
public class KeycloakException extends RuntimeException {

    public KeycloakException(String message) {
        super(message);
    }

    public KeycloakException(String message, Throwable cause) {
        super(message, cause);
    }
}
