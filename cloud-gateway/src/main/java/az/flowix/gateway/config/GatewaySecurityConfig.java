package az.flowix.gateway.config;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewaySecurityConfig.class);

    @Value("${security.oauth2.issuer-uri}")
    private String issuerUri;

    @Value("${security.oauth2.audience:}")
    private String audience;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsWebFilter corsWebFilter(
            @Value("${CORS_ALLOWED_ORIGIN_PATTERNS:http://localhost:3000,http://localhost:5173}") String allowedOriginPatterns) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With",
                "Accept-Language", "X-Internal-Auth"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/api/auth-ms/**",
                                "/swagger-ui/**",
                                "/api/*/swagger-ui/**",
                                "/api/*/v3/api-docs/**",
                                "/favicon.ico"
                        ).permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/menu-ms/v1/images/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/access-ms/v1/permissions/my").permitAll()
                        .pathMatchers("/api/customer-ms/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder()))
                        .authenticationEntryPoint(problemDetailAuthenticationEntryPoint())
                        .accessDeniedHandler(problemDetailAccessDeniedHandler())
                );

        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withIssuerLocation(issuerUri).build();
        OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefaultWithIssuer(issuerUri);

        if (audience != null && !audience.isBlank()) {
            OAuth2TokenValidator<Jwt> audienceValidator = new JwtAudienceValidator(audience);
            decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(defaultValidator, audienceValidator));
        } else {
            decoder.setJwtValidator(defaultValidator);
        }

        return decoder;
    }

    @Bean
    public ServerAuthenticationEntryPoint problemDetailAuthenticationEntryPoint() {
        return (exchange, ex) -> {
            log.warn("Authentication failed for [{}]: {}", exchange.getRequest().getPath(), ex.getMessage());
            return writeProblemDetail(exchange.getResponse(), HttpStatus.UNAUTHORIZED,
                    "Unauthorized", "Authentication is required", "COMMON_4001", exchange);
        };
    }

    @Bean
    public ServerAccessDeniedHandler problemDetailAccessDeniedHandler() {
        return (exchange, denied) -> {
            log.warn("Access denied for [{}]: {}", exchange.getRequest().getPath(), denied.getMessage());
            return writeProblemDetail(exchange.getResponse(), HttpStatus.FORBIDDEN,
                    "Access Denied", "Access is denied", "COMMON_4003", exchange);
        };
    }

    private Mono<Void> writeProblemDetail(ServerHttpResponse response, HttpStatus status,
                                          String title, String detail, String key,
                                          ServerWebExchange exchange) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        String path = exchange.getRequest().getPath().value();
        String json = """
                {"type":"about:blank","title":"%s","status":%d,"detail":"%s","instance":"%s","key":"%s","path":"%s","timestamp":"%s"}"""
                .formatted(title, status.value(), detail, resolveInstance(exchange), key, path, Instant.now());

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private String resolveInstance(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();
        String traceId = firstNonNull(
                headers.getFirst("traceparent"),
                headers.getFirst("X-B3-TraceId"),
                headers.getFirst("X-Trace-Id"),
                headers.getFirst("X-B3-SpanId")
        );
        return traceId != null ? "trace:" + traceId : exchange.getRequest().getPath().value();
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
