package az.flowix.access.repository;

import az.flowix.access.entity.Permission;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID>,
        JpaSpecificationExecutor<Permission> {

    List<Permission> findAllByIdIn(Collection<UUID> ids);

    List<Permission> findAllByDeletedFalseAndIsActiveTrue();

    List<Permission> findAllByCodeInAndDeletedFalseAndIsActiveTrue(Collection<String> codes);

}
