package az.flowix.common.security.context;

import az.flowix.common.security.model.UserPrincipal;

import java.util.Optional;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextFacade {

    private SecurityContextFacade() {
    }

    public static String getCurrentUserId() {
        return getPrincipal().map(UserPrincipal::getUserId).orElse(null);
    }

    public static String requireCurrentUserId() {
        return getPrincipal()
                .map(UserPrincipal::getUserId)
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }

    public static String getCurrentOrgId() {
        return getPrincipal().map(UserPrincipal::getOrgId).orElse(null);
    }

    public static Set<String> getRoles() {
        return getPrincipal().map(UserPrincipal::getRoles).orElse(Set.of());
    }

    public static boolean isPlatformAdmin() {
        return getPrincipal().map(UserPrincipal::isPlatformAdmin).orElse(false);
    }

    public static boolean isSelf(String userId) {
        return getPrincipal()
                .map(p -> p.getUserId() != null && p.getUserId().equals(userId))
                .orElse(false);
    }

    public static boolean belongsToOrg(String orgId) {
        return getPrincipal()
                .map(p -> p.getOrgId() != null && p.getOrgId().equals(orgId))
                .orElse(false);
    }

    public static Optional<UserPrincipal> getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static UserPrincipal requirePrincipal() {
        return getPrincipal()
                .orElseThrow(() -> new IllegalStateException("No authenticated user principal"));
    }

}
