package az.codlab.user.service;

import az.codlab.user.client.KeycloakAdminClient;
import az.codlab.user.client.KeycloakException;
import az.codlab.user.entity.UserRole;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Keeps user lifecycle changes in sync with Keycloak. Create, update and delete
 * operations on the local {@code users} table are mirrored to the Keycloak realm.
 */
@Service
public class KeycloakUserProvisioner {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserProvisioner.class);

    private static final Map<UserRole, String> ROLE_TO_KEYCLOAK_ROLE = Map.of(
            UserRole.ORG_ADMIN, "ORG_ADMIN",
            UserRole.ADMIN, "ADMIN"
    );

    private final KeycloakAdminClient keycloakAdminClient;

    public KeycloakUserProvisioner(KeycloakAdminClient keycloakAdminClient) {
        this.keycloakAdminClient = keycloakAdminClient;
    }

    /**
     * Creates (or reuses) the Keycloak user, sets its password and assigns the
     * matching client role. Returns the Keycloak user id. Idempotent per username.
     */
    public String provision(KeycloakAdminClient.NewUser user, String password, UserRole role) {
        String userId = existingUserId(user.username());
        boolean created = userId == null;
        if (created) {
            userId = keycloakAdminClient.createUser(user);
        }

        try {
            keycloakAdminClient.resetPassword(userId, password);
            assignRole(userId, role, true);
        } catch (RuntimeException ex) {
            if (created) {
                log.warn("Provisioning of Keycloak user '{}' failed, cleaning up", user.username());
                rollback(userId);
            }
            throw ex;
        }
        return userId;
    }

    public void updatePassword(String keycloakId, String password) {
        keycloakAdminClient.resetPassword(keycloakId, password);
    }

    public void updateProfile(String keycloakId, Map<String, Object> fields) {
        keycloakAdminClient.updateUser(keycloakId, fields);
    }

    public void updateRole(String keycloakId, UserRole newRole, UserRole oldRole) {
        assignRole(keycloakId, oldRole, false);
        assignRole(keycloakId, newRole, true);
    }

    public void setActive(String keycloakId, boolean active) {
        keycloakAdminClient.setEnabled(keycloakId, active);
    }

    /**
     * Soft-deletes the local user but only disables the Keycloak account.
     */
    public void deactivate(String keycloakId) {
        keycloakAdminClient.setEnabled(keycloakId, false);
    }

    /**
     * Removes the Keycloak user entirely (used when the local transaction rolls back).
     */
    public void rollback(String keycloakId) {
        try {
            keycloakAdminClient.deleteUser(keycloakId);
        } catch (KeycloakException ex) {
            log.warn("Failed to roll back Keycloak user {}: {}", keycloakId, ex.getMessage());
        }
    }

    private String existingUserId(String username) {
        var users = keycloakAdminClient.findUsersByUsername(username);
        if (users == null || users.isEmpty()) {
            return null;
        }
        return users.get(0).id();
    }

    private void assignRole(String keycloakId, UserRole role, boolean assign) {
        String clientRole = ROLE_TO_KEYCLOAK_ROLE.get(role);
        if (clientRole == null) {
            return;
        }
        if (assign) {
            keycloakAdminClient.assignClientRole(keycloakId, clientRole);
        } else {
            keycloakAdminClient.unassignClientRole(keycloakId, clientRole);
        }
    }
}
