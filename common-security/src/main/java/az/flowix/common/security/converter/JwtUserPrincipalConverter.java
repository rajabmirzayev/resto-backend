package az.flowix.common.security.converter;

import az.flowix.common.enums.UiScope;
import az.flowix.common.security.model.UserPrincipal;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * Converts a JWT (signed by the Keycloak realm) into the same
 * {@link UserPrincipal}-based authentication that {@code HeaderAuthenticationFilter}
 * produces for gateway-forwarded requests, so downstream services can be called
 * directly with a Bearer token as well.
 */
public class JwtUserPrincipalConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final String superAdminRole;

    public JwtUserPrincipalConverter() {
        this("SUPER_ADMIN");
    }

    public JwtUserPrincipalConverter(String superAdminRole) {
        this.superAdminRole = superAdminRole != null ? superAdminRole : "SUPER_ADMIN";
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String userId = jwt.getSubject();
        String orgId = resolveOrgId(jwt);

        List<String> roles = jwt.getClaimAsStringList("roles");
        Set<String> roleSet = (roles == null) ? Set.of() : Set.copyOf(roles);

        boolean platformAdmin = roleSet.contains(superAdminRole)
                || hasRealmRole(jwt, "SUPER_ADMIN");

        List<String> permissionClaims = jwt.getClaimAsStringList("permissions");
        Set<String> permissions = (permissionClaims == null) ? Set.of() : Set.copyOf(permissionClaims);

        UiScope uiScope = parseUiScope(jwt.getClaimAsString("uiScope"));

        UserPrincipal principal = new UserPrincipal(userId, orgId, roleSet, permissions, uiScope, platformAdmin);

        Collection<GrantedAuthority> authorities = new HashSet<>();
        roleSet.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        if (platformAdmin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + superAdminRole));
        }

        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }

    private String resolveOrgId(Jwt jwt) {
        String orgId = jwt.getClaimAsString("organizationId");
        if (!StringUtils.hasText(orgId)) {
            orgId = jwt.getClaimAsString("org_id");
        }
        return StringUtils.hasText(orgId) ? orgId : null;
    }

    private boolean hasRealmRole(Jwt jwt, String role) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> map)) {
            return false;
        }
        Object realmRoles = map.get("roles");
        if (realmRoles instanceof Collection<?> collection) {
            return collection.contains(role);
        }
        return false;
    }

    private UiScope parseUiScope(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UiScope.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
