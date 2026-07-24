package az.codlab.common.security.resolver;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeaderBasedPermissionResolver implements PermissionResolver {

    private static final Logger log = LoggerFactory.getLogger(HeaderBasedPermissionResolver.class);

    @Override
    public Set<String> resolvePermissions(Set<String> roleNames) {
        log.warn("No database-backed PermissionResolver configured. Cannot resolve permissions for roles: {}", roleNames);
        return Set.of();
    }

}
