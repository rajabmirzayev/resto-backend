package az.flowix.access.repository;

import az.flowix.access.entity.User;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> active() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<User> orgId(UUID orgId) {
        if (orgId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("orgId"), orgId);
    }

    public static Specification<User> roleId(UUID roleId) {
        if (roleId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("role").get("id"), roleId);
    }

    public static Specification<User> q(String q) {
        String filter = normalize(q);
        if (filter == null) {
            return null;
        }
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), contains(filter)),
                cb.like(cb.lower(root.get("username")), contains(filter)),
                cb.like(cb.lower(root.get("email")), contains(filter)));
    }

    private static String normalize(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String value = q.trim().toLowerCase(Locale.ROOT);
        return value.isEmpty() ? null : value;
    }

    private static String contains(String value) {
        return "%" + value + "%";
    }

}
