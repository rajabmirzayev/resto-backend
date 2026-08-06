package az.flowix.access.repository;

import az.flowix.access.entity.Role;

import java.util.Locale;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

public final class RoleSpecifications {

    private RoleSpecifications() {
    }

    public static Specification<Role> active() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<Role> q(String q) {
        String filter = normalize(q);
        if (filter == null) {
            return null;
        }
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("code")), contains(filter)),
                cb.like(cb.lower(root.get("name")), contains(filter)));
    }

    /**
     * Tenant isolation: an organization sees its own roles plus global system roles.
     * A null orgId keeps only the global system roles.
     */
    public static Specification<Role> visibleToOrg(UUID orgId) {
        return (root, query, cb) -> {
            var systemGlobal = cb.and(
                    cb.isNull(root.get("orgId")),
                    cb.isTrue(root.get("isSystem")));
            if (orgId == null) {
                return systemGlobal;
            }
            return cb.or(cb.equal(root.get("orgId"), orgId), systemGlobal);
        };
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
