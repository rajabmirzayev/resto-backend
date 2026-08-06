package az.flowix.access.service;

import az.flowix.access.entity.Role;
import az.flowix.access.entity.User;
import az.flowix.access.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Best-effort synchronisation of role-derived user attributes to Keycloak.
 * Failures are logged but must never break the local DB transaction.
 */
@Service
public class KeycloakSyncService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakSyncService.class);

    private final UserRepository userRepository;
    private final KeycloakUserProvisioner keycloakUserProvisioner;

    public KeycloakSyncService(UserRepository userRepository,
                               KeycloakUserProvisioner keycloakUserProvisioner) {
        this.userRepository = userRepository;
        this.keycloakUserProvisioner = keycloakUserProvisioner;
    }

    /**
     * Writes the current role-derived attributes (dbRoles, permissions, uiScope)
     * of the given user to Keycloak. No-op when the user has no Keycloak id.
     */
    public void syncUserRole(User user) {
        if (user.getKeycloakId() == null) {
            return;
        }
        try {
            keycloakUserProvisioner.updateRole(user.getKeycloakId(), user.getRole(), user.getOrgId());
        } catch (Exception ex) {
            log.error("Failed to sync Keycloak attributes for user {}", user.getKeycloakId(), ex);
        }
    }

    /**
     * Clears the role-derived attributes of the given user in Keycloak
     * (used on role unassign / role delete).
     */
    public void clearUserRole(User user) {
        if (user.getKeycloakId() == null) {
            return;
        }
        try {
            keycloakUserProvisioner.clearRoleAttributes(user.getKeycloakId(), user.getOrgId());
        } catch (Exception ex) {
            log.error("Failed to clear Keycloak attributes for user {}", user.getKeycloakId(), ex);
        }
    }

    /**
     * Re-synchronises all users assigned to the given role (used after a role
     * permission or uiScope change).
     */
    public void syncUsersOfRole(Role role) {
        if (role == null || role.getId() == null) {
            return;
        }
        var users = userRepository.findAllByRole_IdAndDeletedFalse(role.getId());
        for (var user : users) {
            syncUserRole(user);
        }
    }
}
