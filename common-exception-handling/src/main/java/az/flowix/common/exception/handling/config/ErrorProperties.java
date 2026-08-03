package az.flowix.common.exception.handling.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "common.error")
public class ErrorProperties {

    private String serviceKey;

}