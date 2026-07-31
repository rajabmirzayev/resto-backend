package az.codlab.common.security.converter;

import az.codlab.common.security.model.UserPrincipal;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Converts a JWT (signed by the Keycloak realm) into the same
 * {@link UserPrincipal}-based authentication that {@code HeaderAuthenticationFilter}
 * produces for gateway-forwarded requests, so downstream services can be called
 * directly with a Bearer token as well.
 */
public class JwtUserPrincipalConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String userId = jwt.getSubject();
        String orgId = jwt.getClaimAsString("organizationId");

        List<String> roles = jwt.getClaimAsStringList("roles");
        Set<String> roleSet = (roles == null) ? Set.of() : Set.copyOf(roles);
        boolean platformAdmin = roleSet.contains("SUPER_ADMIN");

        UserPrincipal principal = new UserPrincipal(userId, orgId, roleSet, platformAdmin);

        Collection<GrantedAuthority> authorities = new HashSet<>();
        roleSet.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        if (platformAdmin) {
            authorities.add(new SimpleGrantedAuthority("SUPER_ADMIN"));
        }

        return new UsernamePasswordAuthenticationToken(principal, jwt, authorities);
    }
}
