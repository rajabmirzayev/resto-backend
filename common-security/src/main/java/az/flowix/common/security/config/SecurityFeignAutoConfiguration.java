package az.flowix.common.security.config;

import az.flowix.common.security.feign.HeaderPropagationRequestInterceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import feign.RequestInterceptor;

@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class SecurityFeignAutoConfiguration {

    @Bean
    public HeaderPropagationRequestInterceptor headerPropagationRequestInterceptor(
            @Value("${security.internal-auth.secret:}") String internalAuthSecret) {
        return new HeaderPropagationRequestInterceptor(internalAuthSecret);
    }

}
