package az.flowix.common.exception.handling.config;

import az.flowix.common.exception.handling.decoder.CommonFeignErrorDecoder;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableConfigurationProperties(ErrorProperties.class)
public class ExceptionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ErrorDecoder.class)
    public ErrorDecoder feignErrorDecoder(ObjectMapper objectMapper) {
        return new CommonFeignErrorDecoder(objectMapper);
    }

}
