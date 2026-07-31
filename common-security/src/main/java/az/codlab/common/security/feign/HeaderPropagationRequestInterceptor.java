package az.codlab.common.security.feign;

import az.codlab.common.security.filter.HeaderAuthenticationFilter;
import az.codlab.common.security.model.UserPrincipal;

import java.util.Enumeration;
import java.util.Set;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propagates the authenticated user's identity to outgoing Feign client calls so
 * downstream services can build the same SecurityContext as the origin of the
 * request. The identity is taken from the current {@link UserPrincipal} (produced
 * either from gateway-forwarded headers or from a directly presented JWT), falling
 * back to the incoming request headers.
 */
public class HeaderPropagationRequestInterceptor implements RequestInterceptor {

    private static final String[] PROPAGATED_HEADERS = {
            HeaderAuthenticationFilter.HEADER_USER_ID,
            HeaderAuthenticationFilter.HEADER_ORG_ID,
            HeaderAuthenticationFilter.HEADER_ROLES,
            HeaderAuthenticationFilter.HEADER_PLATFORM_ADMIN
    };

    @Override
    public void apply(RequestTemplate requestTemplate) {
        UserPrincipal principal = currentPrincipal();

        for (var header : PROPAGATED_HEADERS) {
            if (requestTemplate.headers().containsKey(header)) {
                continue;
            }

            if (principal != null) {
                String value = valueFor(header, principal);
                if (value != null) {
                    requestTemplate.header(header, value);
                    continue;
                }
            }

            copyHeader(requestTemplate, header);
        }
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }

    private String valueFor(String header, UserPrincipal principal) {
        return switch (header) {
            case HeaderAuthenticationFilter.HEADER_USER_ID -> principal.getUserId();
            case HeaderAuthenticationFilter.HEADER_ORG_ID -> principal.getOrgId();
            case HeaderAuthenticationFilter.HEADER_ROLES -> {
                Set<String> roles = principal.getRoles();
                yield (roles == null || roles.isEmpty()) ? null : String.join(",", roles);
            }
            case HeaderAuthenticationFilter.HEADER_PLATFORM_ADMIN -> String.valueOf(principal.isPlatformAdmin());
            default -> null;
        };
    }

    private void copyHeader(RequestTemplate requestTemplate, String name) {
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return;
        }

        var request = servletAttributes.getRequest();
        Enumeration<String> values = request.getHeaders(name);
        if (values != null && values.hasMoreElements()) {
            while (values.hasMoreElements()) {
                requestTemplate.header(name, values.nextElement());
            }
        }
    }
}
