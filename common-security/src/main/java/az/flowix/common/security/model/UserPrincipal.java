package az.flowix.common.security.model;

import java.util.Set;

public final class UserPrincipal {

    private final String userId;
    private final String orgId;
    private final Set<String> roles;
    private final boolean platformAdmin;

    public UserPrincipal(String userId,
                         String orgId,
                         Set<String> roles,
                         boolean platformAdmin) {
        this.userId = userId;
        this.orgId = orgId;
        this.roles = roles != null ? Set.copyOf(roles) : Set.of();
        this.platformAdmin = platformAdmin || this.roles.contains("SUPER_ADMIN");
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

    public boolean isPlatformAdmin() {
        return platformAdmin;
    }

    @Override
    public String toString() {
        return "UserPrincipal{userId='" + userId + "', orgId='" + orgId + "', platformAdmin=" + platformAdmin + "}";
    }

}
