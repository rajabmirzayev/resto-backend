package az.codlab.common.security.filter;

import az.codlab.common.security.model.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_ORG_ID = "X-Org-Id";
    public static final String HEADER_ROLES = "X-Roles";
    public static final String HEADER_PLATFORM_ADMIN = "X-Platform-Admin";
    public static final String HEADER_INTERNAL_AUTH = "X-Internal-Auth";

    private final String internalAuthSecret;

    public HeaderAuthenticationFilter() {
        this(null);
    }

    public HeaderAuthenticationFilter(String internalAuthSecret) {
        this.internalAuthSecret = internalAuthSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null
                && isTrustedInternalRequest(request)) {
            var userId = request.getHeader(HEADER_USER_ID);

            if (userId != null && !userId.isBlank()) {
                var orgId = request.getHeader(HEADER_ORG_ID);
                var roles = parseSet(request.getHeader(HEADER_ROLES));
                var platformAdmin = Boolean.TRUE.toString().equalsIgnoreCase(request.getHeader(HEADER_PLATFORM_ADMIN));

                var principal = new UserPrincipal(userId, orgId, roles, platformAdmin);

                var authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toSet());

                if (platformAdmin) {
                    authorities.add(new SimpleGrantedAuthority("SUPER_ADMIN"));
                }

                var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                var internalPrincipal = new UserPrincipal(null, null, Set.of(), false);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(internalPrincipal, null, Set.of()));
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTrustedInternalRequest(HttpServletRequest request) {
        if (!StringUtils.hasText(internalAuthSecret)) {
            return false;
        }
        var presented = request.getHeader(HEADER_INTERNAL_AUTH);
        if (presented == null) {
            return false;
        }
        return MessageDigest.isEqual(
                internalAuthSecret.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
    }

    private Set<String> parseSet(String header) {
        if (header == null || header.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

}
