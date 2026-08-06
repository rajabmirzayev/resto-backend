package az.flowix.auth.service;

import az.flowix.auth.client.KeycloakClient;
import az.flowix.auth.dto.LoginRequest;
import az.flowix.auth.dto.LoginResponse;
import az.flowix.auth.dto.LogoutRequest;
import az.flowix.auth.dto.RefreshRequest;
import az.flowix.auth.dto.RefreshResponse;
import az.flowix.auth.security.JwtTokenValidator;
import az.flowix.common.enums.UiScope;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class KeycloakAuthService implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthService.class);

    private final KeycloakClient keycloakClient;
    private final JwtTokenValidator jwtTokenValidator;
    private final String platformAdminRole;

    public KeycloakAuthService(KeycloakClient keycloakClient,
                               JwtTokenValidator jwtTokenValidator,
                               @Value("${auth.super-admin-role}") String platformAdminRole) {
        this.keycloakClient = keycloakClient;
        this.jwtTokenValidator = jwtTokenValidator;
        this.platformAdminRole = platformAdminRole;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user '{}'", request.username());

        var tokenResponse = keycloakClient.login(request.username(), request.password());

        var claims = jwtTokenValidator.extractClaims(tokenResponse.accessToken());
        var roles = stringList(claims.get("roles"));
        var uiScope = resolveUiScope(claims);
        var permissions = stringList(claims.get("permissions"));

        log.info("Login successful for user '{}', uiScope={}", request.username(), uiScope);

        return new LoginResponse(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.expiresIn(),
                tokenResponse.tokenType(),
                new LoginResponse.User(request.username(), roles),
                uiScope,
                permissions
        );
    }

    @Override
    public RefreshResponse refresh(RefreshRequest request) {
        log.debug("Token refresh requested");

        var tokenResponse = keycloakClient.refresh(request.refreshToken());

        log.debug("Token refresh successful");

        return new RefreshResponse(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.expiresIn()
        );
    }

    @Override
    public void logout(LogoutRequest request) {
        log.info("Logout requested");

        keycloakClient.logout(request.refreshToken());

        log.info("Logout successful");
    }

    /**
     * Resolves the UI scope for the login response:
     * <ol>
     *   <li>the {@code uiScope} claim from the access token (driven by the role attributes),</li>
     *   <li>else {@code SUPER_ADMIN_PANEL} when the {@code roles} claim contains the platform admin role,</li>
     *   <li>else {@code ADMIN_PANEL} (legacy default).</li>
     * </ol>
     */
    private UiScope resolveUiScope(Map<String, Object> claims) {
        var scope = claims.get("uiScope");
        if (scope instanceof String value && !value.isBlank()) {
            try {
                return UiScope.valueOf(value);
            } catch (IllegalArgumentException ex) {
                log.warn("Unknown uiScope claim '{}', falling back to role-based resolution", value);
            }
        }

        if (stringList(claims.get("roles")).contains(platformAdminRole)) {
            return UiScope.SUPER_ADMIN_PANEL;
        }

        return UiScope.ADMIN_PANEL;
    }

    private List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

}
