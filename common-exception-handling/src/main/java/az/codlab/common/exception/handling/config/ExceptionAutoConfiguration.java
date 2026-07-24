package az.codlab.common.exception.handling.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ErrorProperties.class)
public class ExceptionAutoConfiguration {
}
