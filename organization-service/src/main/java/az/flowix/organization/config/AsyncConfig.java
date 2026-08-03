package az.flowix.organization.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.concurrent.Executor;

/**
 * Bounded executor for fan-out calls to downstream services. Copies the caller's
 * security context and request headers into worker threads so that Feign requests
 * made in the background keep the same identity as the originating request.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "organizationProvisioningExecutor")
    public Executor organizationProvisioningExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("org-provision-");
        executor.setTaskDecorator(contextDecorator());
        executor.initialize();
        return executor;
    }

    private TaskDecorator contextDecorator() {
        return runnable -> {
            var securityContext = SecurityContextHolder.getContextHolderStrategy().getContext();
            var requestAttributes = RequestContextHolder.getRequestAttributes();
            return () -> {
                var previousSecurityContext = SecurityContextHolder.getContextHolderStrategy().getContext();
                var previousRequestAttributes = RequestContextHolder.getRequestAttributes();
                try {
                    SecurityContextHolder.getContextHolderStrategy().setContext(securityContext);
                    RequestContextHolder.setRequestAttributes(requestAttributes);
                    runnable.run();
                } finally {
                    RequestContextHolder.setRequestAttributes(previousRequestAttributes);
                    SecurityContextHolder.getContextHolderStrategy().setContext(previousSecurityContext);
                }
            };
        };
    }
}
