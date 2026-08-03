package az.flowix.common.jpa.config;

import az.flowix.common.security.context.SecurityContextFacade;

import java.util.Optional;
import java.util.UUID;

import lombok.NonNull;
import org.springframework.data.domain.AuditorAware;

public class AuditorAwareImpl implements AuditorAware<UUID> {

    @Override
    @NonNull
    public Optional<UUID> getCurrentAuditor() {
        var userId = SecurityContextFacade.getCurrentUserId();
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(userId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

}
