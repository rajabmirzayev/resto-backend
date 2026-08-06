package az.flowix.access.security;

import az.flowix.access.entity.Permission;
import az.flowix.access.entity.Role;
import az.flowix.access.repository.RoleRepository;
import az.flowix.common.security.resolver.PermissionResolver;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class JpaPermissionResolver implements PermissionResolver {

    private final RoleRepository roleRepository;

    public JpaPermissionResolver(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Set<String> resolvePermissions(Set<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of();
        }
        List<Role> roles = roleRepository.findAllByCodeInWithPermissions(roleNames);
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

}
