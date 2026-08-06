package az.flowix.common.security.config;

import az.flowix.common.exception.handling.model.TraceHeaders;
import az.flowix.common.security.access.PermissionEvaluator;
import az.flowix.common.security.converter.JwtUserPrincipalConverter;
import az.flowix.common.security.filter.HeaderAuthenticationFilter;
import az.flowix.common.security.resolver.HeaderBasedPermissionResolver;
import az.flowix.common.security.resolver.PermissionResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AutoConfigureBefore(name = "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration")
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            HeaderAuthenticationFilter headerAuthenticationFilter,
            JwtUserPrincipalConverter jwtUserPrincipalConverter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/error",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/v1/auth/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtUserPrincipalConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @ConditionalOnMissingBean(PermissionResolver.class)
    public PermissionResolver permissionResolver() {
        return new HeaderBasedPermissionResolver();
    }

    @Bean("perm")
    @ConditionalOnMissingBean(PermissionEvaluator.class)
    public PermissionEvaluator permissionEvaluator(PermissionResolver permissionResolver) {
        return new PermissionEvaluator(permissionResolver);
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8080/realms/resto}")
            String issuerUri) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(issuerUri + "/protocol/openid-connect/certs")
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean(HeaderAuthenticationFilter.class)
    public HeaderAuthenticationFilter headerAuthenticationFilter(
            @Value("${security.internal-auth.secret:}") String internalAuthSecret) {
        return new HeaderAuthenticationFilter(internalAuthSecret);
    }

    @Bean
    @ConditionalOnMissingBean(JwtUserPrincipalConverter.class)
    public JwtUserPrincipalConverter jwtUserPrincipalConverter() {
        return new JwtUserPrincipalConverter();
    }

    @Bean
    @ConditionalOnMissingBean(AuthenticationEntryPoint.class)
    public AuthenticationEntryPoint problemDetailAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            log.debug("Authentication failed for [{}]: {}", request.getRequestURI(), authException.getMessage());
            writeProblemDetail(response, HttpStatus.UNAUTHORIZED,
                    "Unauthorized", "Authentication is required", "COMMON_4001", request);
        };
    }

    @Bean
    @ConditionalOnMissingBean(AccessDeniedHandler.class)
    public AccessDeniedHandler problemDetailAccessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            log.debug("Access denied for [{}]: {}", request.getRequestURI(), accessDeniedException.getMessage());
            writeProblemDetail(response, HttpStatus.FORBIDDEN,
                    "Access Denied", "Access is denied", "COMMON_4003", request);
        };
    }

    private void writeProblemDetail(HttpServletResponse response,
                                    HttpStatus status, String title, String detail, String key,
                                    HttpServletRequest request) {
        try {
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            String json = """
                    {"type":"about:blank","title":"%s","status":%d,"detail":"%s","instance":"%s","key":"%s","path":"%s","timestamp":"%s"}"""
                    .formatted(title, status.value(), detail, resolveInstance(request), key,
                            request.getRequestURI(), Instant.now());
            PrintWriter writer = response.getWriter();
            writer.write(json);
            writer.flush();
        } catch (IOException e) {
            log.error("Failed to write error response", e);
        }
    }

    private String resolveInstance(HttpServletRequest request) {
        String traceId = firstNonNull(
                request.getHeader(TraceHeaders.TRACEPARENT),
                request.getHeader(TraceHeaders.X_B3_TRACE_ID),
                request.getHeader(TraceHeaders.X_TRACE_ID),
                request.getHeader(TraceHeaders.X_B3_SPAN_ID)
        );
        return traceId != null ? "trace:" + traceId : request.getRequestURI();
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

}
