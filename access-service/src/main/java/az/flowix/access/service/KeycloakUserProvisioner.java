package az.flowix.access.service;

import az.flowix.access.client.KeycloakAdminClient;
import az.flowix.access.client.KeycloakException;
import az.flowix.access.entity.Permission;
import az.flowix.access.entity.Role;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Keeps user lifecycle changes in sync with Keycloak. Create, update and delete
 * operations on the local {@code users} table are mirrored to the Keycloak realm.
 *
 * <p>Since the role-based permissions model, the Keycloak user only receives
 * user {@code attributes} ({@code roles}, {@code permissions}, {@code uiScope})
 * which the realm protocol mappers expose as JWT claims ({@code dbRoles},
 * {@code permissions}, {@code uiScope}). No client roles are assigned from here
 * anymore.
 */
@Service
public class KeycloakUserProvisioner {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserProvisioner.class);

    public static final String ATTR_ROLES = "roles";
    public static final String ATTR_PERMISSIONS = "permissions";
    public static final String ATTR_UI_SCOPE = "uiScope";
    public static final String ATTR_ORG_ID = "organizationId";

    private final KeycloakAdminClient keycloakAdminClient;

    public KeycloakUserProvisioner(KeycloakAdminClient keycloakAdminClient) {
        this.keycloakAdminClient = keycloakAdminClient;
    }

    /**
     * Creates (or reuses) the Keycloak user, sets its password and writes the
     * role-derived attributes. Returns the Keycloak user id. Idempotent per username.
     */
    public String provision(KeycloakAdminClient.NewUser user, String password, Role role) {
        String userId = existingUserId(user.username());
        boolean created = userId == null;
        if (created) {
            userId = keycloakAdminClient.createUser(user);
        }

        try {
            keycloakAdminClient.resetPassword(userId, password);
            setRoleAttributes(userId, role, user.orgId());
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

    public void updateRole(String keycloakId, Role role, UUID orgId) {
        setRoleAttributes(keycloakId, role, orgId);
    }

    public void clearRoleAttributes(String keycloakId, UUID orgId) {
        keycloakAdminClient.setAttributes(keycloakId, Map.of(
                ATTR_ROLES, List.of(),
                ATTR_PERMISSIONS, List.of(),
                ATTR_UI_SCOPE, List.of(),
                ATTR_ORG_ID, orgIdList(orgId)));
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

    public Map<String, List<String>> roleAttributes(Role role, UUID orgId) {
        if (role == null) {
            return Map.of(ATTR_ORG_ID, orgIdList(orgId));
        }
        List<String> permissions = role.getPermissions() == null ? List.of()
                : role.getPermissions().stream().map(Permission::getCode).sorted().toList();
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put(ATTR_ROLES, List.of(role.getCode()));
        attributes.put(ATTR_PERMISSIONS, permissions);
        attributes.put(ATTR_UI_SCOPE, List.of(role.getUiScope().name()));
        attributes.put(ATTR_ORG_ID, orgIdList(orgId));
        return attributes;
    }

    private void setRoleAttributes(String keycloakId, Role role, UUID orgId) {
        keycloakAdminClient.setAttributes(keycloakId, roleAttributes(role, orgId));
    }

    private List<String> orgIdList(UUID orgId) {
        return orgId == null ? List.of() : List.of(orgId.toString());
    }

    private String existingUserId(String username) {
        var users = keycloakAdminClient.findUsersByUsername(username);
        if (users == null || users.isEmpty()) {
            return null;
        }
        return users.get(0).id();
    }
}
