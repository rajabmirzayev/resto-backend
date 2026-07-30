package az.codlab.report.config;

import az.codlab.common.security.filter.HeaderAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ReportSecurityConfig {

    @Bean
    public HeaderAuthenticationFilter headerAuthenticationFilter() { return new HeaderAuthenticationFilter(); }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HeaderAuthenticationFilter f) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable).formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.requestMatchers("/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**","/swagger-resources/**","/webjars/**").permitAll()
                        // TODO: endpoint-leri role gore qoru; hazirliq ucun .permitAll()
                        .anyRequest().permitAll())
                .addFilterBefore(f, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
