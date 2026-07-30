package az.codlab.organization.repository;

import az.codlab.organization.entity.LocalRole;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalRoleRepository extends JpaRepository<LocalRole, UUID> {

    Optional<LocalRole> findByIdAndDeletedFalse(UUID id);

    boolean existsByNameAndOrgIdAndDeletedFalse(String name, UUID orgId);

}
