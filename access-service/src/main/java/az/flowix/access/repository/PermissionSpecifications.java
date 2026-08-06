package az.flowix.access.repository;

import az.flowix.access.entity.Permission;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

public final class PermissionSpecifications {

    private PermissionSpecifications() {
    }

    public static Specification<Permission> active() {
        return (root, query, cb) -> cb.and(
                cb.isFalse(root.get("deleted")),
                cb.isTrue(root.get("isActive")));
    }

    public static Specification<Permission> q(String q) {
        String filter = normalize(q);
        if (filter == null) {
            return null;
        }
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), contains(filter)),
                cb.like(cb.lower(root.get("name")), contains(filter)));
    }

    public static Specification<Permission> moduleId(UUID moduleId) {
        if (moduleId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("moduleId"), moduleId);
    }

    public static Specification<Permission> uiGroupId(UUID uiGroupId) {
        if (uiGroupId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("uiGroupId"), uiGroupId);
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
