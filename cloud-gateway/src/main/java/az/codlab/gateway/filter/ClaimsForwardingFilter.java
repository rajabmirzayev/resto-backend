package az.codlab.gateway.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class ClaimsForwardingFilter implements GlobalFilter, Ordered {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_ORG_ID = "X-Org-Id";
    public static final String HEADER_ROLES = "X-Roles";
    public static final String HEADER_PLATFORM_ADMIN = "X-Platform-Admin";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .flatMap(auth -> {
                    if (auth instanceof JwtAuthenticationToken jwtAuth) {
                        Jwt jwt = jwtAuth.getToken();

                        String userId = jwt.getSubject();
                        String organizationId = jwt.getClaimAsString("organizationId");
                        List<String> roles = resolveRoles(jwt);
                        boolean platformAdmin = roles.contains("SUPER_ADMIN");

                        var mutatedRequest = exchange.getRequest().mutate()
                                .headers(headers -> {
                                    headers.remove(HttpHeaders.AUTHORIZATION);

                                    setIfNotBlank(headers, HEADER_USER_ID, userId);
                                    setIfNotBlank(headers, HEADER_ORG_ID, organizationId);
                                    setIfNotBlank(headers, HEADER_ROLES, toCsv(roles));
                                    headers.set(HEADER_PLATFORM_ADMIN, String.valueOf(platformAdmin));
                                })
                                .build();

                        return chain.filter(exchange.mutate().request(mutatedRequest).build());
                    }
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 1;
    }

    @SuppressWarnings("unchecked")
    private List<String> resolveRoles(Jwt jwt) {
        List<String> userRoles = new ArrayList<>();

        Object realmRoles = jwt.getClaim("roles");
        if (realmRoles instanceof List<?> roleList) {
            roleList.forEach(role -> {
                if (role != null) {
                    userRoles.add(String.valueOf(role));
                }
            });
        }

        return userRoles;
    }

    private String toCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        StringJoiner joiner = new StringJoiner(",");
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                joiner.add(value.trim());
            }
        }
        String result = joiner.toString();
        return result.isBlank() ? null : result;
    }

    private void setIfNotBlank(HttpHeaders headers, String name, String value) {
        if (value != null && !value.isBlank()) {
            headers.set(name, value);
        }
    }

}
