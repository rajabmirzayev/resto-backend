package az.flowix.role.repository;

import az.flowix.role.entity.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    List<Role> findAllByDeletedFalseOrderByCreatedAtDesc();

    Optional<Role> findByIdAndDeletedFalse(UUID id);

    List<Role> findAllByOrgIdAndDeletedFalse(UUID orgId);

}
