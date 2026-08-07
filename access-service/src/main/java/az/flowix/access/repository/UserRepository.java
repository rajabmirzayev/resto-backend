package az.flowix.access.repository;

import az.flowix.access.entity.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    List<User> findAllByDeletedFalseOrderByCreatedAtDesc();

    Optional<User> findByIdAndDeletedFalse(UUID id);

    List<User> findAllByOrgIdAndDeletedFalse(UUID orgId);

    List<User> findAllByOrgIdAndRole_IdAndDeletedFalse(UUID orgId, UUID roleId);

    List<User> findAllByRole_IdAndDeletedFalse(UUID roleId);

    List<User> findAllByIdInAndDeletedFalse(Collection<UUID> ids);

    @Query("select distinct u from User u left join fetch u.role where u.id in :ids")
    List<User> findAllByIdInWithRole(@Param("ids") Collection<UUID> ids);

    boolean existsByUsernameAndDeletedFalse(String username);

    Optional<User> findByUsernameAndDeletedFalse(String username);

    @Query("""
            select u from User u
            where u.deleted = false and u.role.id = :roleId
              and (lower(u.name) like lower(concat('%', :q, '%'))
                   or lower(u.username) like lower(concat('%', :q, '%'))
                   or lower(u.email) like lower(concat('%', :q, '%')))
            """)
    Page<User> searchByRole(@Param("roleId") UUID roleId, @Param("q") String q, Pageable pageable);

}
