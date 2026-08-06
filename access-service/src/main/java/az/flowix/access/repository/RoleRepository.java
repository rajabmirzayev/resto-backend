package az.flowix.access.repository;

import az.flowix.access.entity.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID>, JpaSpecificationExecutor<Role> {

    List<Role> findAllByDeletedFalseOrderByCreatedAtDesc();

    Optional<Role> findByIdAndDeletedFalse(UUID id);

    List<Role> findAllByOrgIdAndDeletedFalse(UUID orgId);

    List<Role> findAllByOrgIdIsNullAndDeletedFalse();

    Optional<Role> findByCodeAndOrgIdIsNullAndDeletedFalse(String code);

    Optional<Role> findByCodeAndOrgIdAndDeletedFalse(String code, UUID orgId);

    boolean existsByCodeAndOrgIdAndDeletedFalse(String code, UUID orgId);

    boolean existsByCodeAndOrgIdIsNullAndDeletedFalse(String code);

    @Query("select distinct r from Role r left join fetch r.permissions where r.deleted = false and r.code in :codes")
    List<Role> findAllByCodeInWithPermissions(@Param("codes") Collection<String> codes);

    @Query("select distinct r from Role r left join fetch r.permissions where r.id in :ids")
    List<Role> findAllByIdInWithPermissions(@Param("ids") Collection<UUID> ids);

}
