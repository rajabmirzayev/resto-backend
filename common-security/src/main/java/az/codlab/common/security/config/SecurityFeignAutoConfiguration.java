package az.codlab.common.security.config;

import az.codlab.common.security.feign.HeaderPropagationRequestInterceptor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import feign.RequestInterceptor;

@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class SecurityFeignAutoConfiguration {

    @Bean
    public HeaderPropagationRequestInterceptor headerPropagationRequestInterceptor() {
        return new HeaderPropagationRequestInterceptor();
    }

}
