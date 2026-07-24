package az.codlab.common.security.resolver;

import java.util.Set;

public interface PermissionResolver {

    Set<String> resolvePermissions(Set<String> roleNames);

    default boolean hasPermission(Set<String> roleNames, String permission) {
        return resolvePermissions(roleNames).contains(permission);
    }

    default boolean hasAnyPermission(Set<String> roleNames, String... permissions) {
        Set<String> resolved = resolvePermissions(roleNames);
        for (String permission : permissions) {
            if (resolved.contains(permission)) {
                return true;
            }
        }
        return false;
    }

}
