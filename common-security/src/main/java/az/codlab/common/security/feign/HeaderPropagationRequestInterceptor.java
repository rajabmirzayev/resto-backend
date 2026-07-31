package az.codlab.common.security.feign;

import az.codlab.common.security.filter.HeaderAuthenticationFilter;

import java.util.Enumeration;

import feign.RequestInterceptor;
import feign.RequestTemplate;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Propagates the authenticated user's identity headers from the incoming request
 * to outgoing Feign client calls so downstream services can build the same
 * SecurityContext as the origin of the request.
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
        var attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return;
        }

        var request = servletAttributes.getRequest();
        for (var header : PROPAGATED_HEADERS) {
            copyHeader(request, requestTemplate, header);
        }
    }

    private void copyHeader(jakarta.servlet.http.HttpServletRequest request,
                            RequestTemplate requestTemplate,
                            String name) {
        if (requestTemplate.headers().containsKey(name)) {
            return;
        }
        Enumeration<String> values = request.getHeaders(name);
        if (values != null && values.hasMoreElements()) {
            while (values.hasMoreElements()) {
                requestTemplate.header(name, values.nextElement());
            }
        }
    }

}
