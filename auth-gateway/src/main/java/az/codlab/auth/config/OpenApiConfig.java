package az.codlab.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Gateway API")
                        .version("1.0.0")
                        .description("""
                                Authentication gateway for the HR SaaS platform.
                                
                                Acts as an auth orchestrator between the frontend and Keycloak.
                                The frontend **never** contacts Keycloak directly.
                                
                                Endpoints:
                                - `POST /auth/login`   — Authenticate with username/password
                                - `POST /auth/refresh` — Refresh an expired access token
                                - `POST /auth/logout`  — Invalidate session / revoke tokens
                                """)
                        .contact(new Contact()
                                .name("Codlab Team")
                                .email("dev@codlab.az")));
    }

}
