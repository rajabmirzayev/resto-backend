package az.flowix.common.security.model;

import az.flowix.common.enums.UiScope;

import java.util.Set;

public final class UserPrincipal {

    private final String userId;
    private final String orgId;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final UiScope uiScope;
    private final boolean platformAdmin;

    public UserPrincipal(String userId,
                         String orgId,
                         Set<String> roles,
                         boolean platformAdmin) {
        this(userId, orgId, roles, Set.of(), null, platformAdmin);
    }

    public UserPrincipal(String userId,
                         String orgId,
                         Set<String> roles,
                         Set<String> permissions,
                         UiScope uiScope,
                         boolean platformAdmin) {
        this.userId = userId;
        this.orgId = orgId;
        this.roles = roles != null ? Set.copyOf(roles) : Set.of();
        this.permissions = permissions != null ? Set.copyOf(permissions) : Set.of();
        this.uiScope = uiScope;
        this.platformAdmin = platformAdmin;
    }

    public String getUserId() {
        return userId;
    }

    public String getOrgId() {
        return orgId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public UiScope getUiScope() {
        return uiScope;
    }

    public boolean isPlatformAdmin() {
        return platformAdmin;
    }

    public boolean hasPermission(String permission) {
        return platformAdmin || permissions.contains(permission);
    }

    @Override
    public String toString() {
        return "UserPrincipal{userId='" + userId + "', orgId='" + orgId
                + "', uiScope=" + uiScope + ", platformAdmin=" + platformAdmin + "}";
    }

}
