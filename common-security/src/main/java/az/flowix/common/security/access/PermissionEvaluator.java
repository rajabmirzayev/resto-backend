package az.flowix.common.security.access;

import az.flowix.common.security.model.UserPrincipal;
import az.flowix.common.security.resolver.HeaderBasedPermissionResolver;
import az.flowix.common.security.resolver.PermissionResolver;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class PermissionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(PermissionEvaluator.class);

    private final PermissionResolver permissionResolver;

    public PermissionEvaluator(PermissionResolver permissionResolver) {
        if (permissionResolver != null) {
            this.permissionResolver = permissionResolver;
            log.info("Using database-backed PermissionResolver: {}", permissionResolver.getClass().getSimpleName());
        } else {
            this.permissionResolver = new HeaderBasedPermissionResolver();
            log.info("Using header-based PermissionResolver (no DB resolver configured)");
        }
    }

    public boolean has(String permission) {
        return has(SecurityContextHolder.getContext().getAuthentication(), permission);
    }

    public boolean hasAny(String... permissions) {
        return hasAny(SecurityContextHolder.getContext().getAuthentication(), permissions);
    }

    public boolean has(Authentication auth, String permission) {
        UserPrincipal principal = getPrincipal(auth);
        if (principal == null) {
            return false;
        }
        if (principal.isPlatformAdmin()) {
            return true;
        }

        return resolveAndCheck(principal, permission);
    }

    public boolean hasAny(Authentication auth, String... permissions) {
        UserPrincipal principal = getPrincipal(auth);
        if (principal == null) {
            return false;
        }
        if (principal.isPlatformAdmin()) {
            return true;
        }

        return resolveAndCheckAny(principal, permissions);
    }

    public boolean hasAuthority(Authentication auth, String authority) {
        return has(auth, authority);
    }

    public boolean selfOrHas(Authentication auth, String resourceUserId, String permission) {
        UserPrincipal principal = getPrincipal(auth);
        if (principal == null) {
            return false;
        }
        if (principal.isPlatformAdmin()) {
            return true;
        }
        if (principal.getUserId() != null && principal.getUserId().equals(resourceUserId)) {
            return true;
        }
        return resolveAndCheck(principal, permission);
    }

    public boolean selfWith(Authentication auth, String resourceUserId,
                            String selfPermission, String fullPermission) {
        UserPrincipal principal = getPrincipal(auth);
        if (principal == null) {
            return false;
        }
        if (principal.isPlatformAdmin()) {
            return true;
        }
        if (principal.getUserId() != null && principal.getUserId().equals(resourceUserId)
                && resolveAndCheck(principal, selfPermission)) {
            return true;
        }
        return resolveAndCheck(principal, fullPermission);
    }

    public boolean forOrg(Authentication auth, String resourceOrgId, String permission) {
        UserPrincipal principal = getPrincipal(auth);
        if (principal == null) {
            return false;
        }
        if (principal.isPlatformAdmin()) {
            return true;
        }
        String userOrgId = principal.getOrgId();
        if (resourceOrgId != null && userOrgId != null && !resourceOrgId.equals(userOrgId)) {
            return false;
        }
        return resolveAndCheck(principal, permission);
    }

    private boolean resolveAndCheck(UserPrincipal principal, String permission) {
        Set<String> permissions = principal.getPermissions();
        if (!permissions.isEmpty()) {
            return permissions.contains(permission);
        }

        Set<String> roles = principal.getRoles();
        if (roles.isEmpty()) {
            return false;
        }
        return permissionResolver.hasPermission(roles, permission);
    }

    private boolean resolveAndCheckAny(UserPrincipal principal, String... permissionsToCheck) {
        Set<String> permissions = principal.getPermissions();
        if (!permissions.isEmpty()) {
            for (String permission : permissionsToCheck) {
                if (permissions.contains(permission)) {
                    return true;
                }
            }
            return false;
        }

        Set<String> roles = principal.getRoles();
        if (roles.isEmpty()) {
            return false;
        }
        return permissionResolver.hasAnyPermission(roles, permissionsToCheck);
    }

    private UserPrincipal getPrincipal(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        if (auth.getPrincipal() instanceof UserPrincipal up) {
            return up;
        }
        return null;
    }

}
