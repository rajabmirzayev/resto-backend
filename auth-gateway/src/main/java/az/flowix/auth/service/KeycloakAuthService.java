package az.flowix.auth.service;

import az.flowix.auth.client.KeycloakClient;
import az.flowix.auth.dto.LoginRequest;
import az.flowix.auth.dto.LoginResponse;
import az.flowix.auth.dto.LogoutRequest;
import az.flowix.auth.dto.RefreshRequest;
import az.flowix.auth.dto.RefreshResponse;
import az.flowix.auth.enums.UiScope;
import az.flowix.auth.security.JwtTokenValidator;

import java.util.List;

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

        var roles = jwtTokenValidator.extractRoles(tokenResponse.accessToken());
        var uiScope = resolveUiScope(roles);

        log.info("Login successful for user '{}', uiScope={}", request.username(), uiScope);

        return new LoginResponse(
                tokenResponse.accessToken(),
                tokenResponse.refreshToken(),
                tokenResponse.expiresIn(),
                roles,
                uiScope
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

    private UiScope resolveUiScope(List<String> roles) {
        if (roles.contains(platformAdminRole)) {
            return UiScope.ADMIN_PANEL;
        }
        return UiScope.USER_PANEL;
    }

}
