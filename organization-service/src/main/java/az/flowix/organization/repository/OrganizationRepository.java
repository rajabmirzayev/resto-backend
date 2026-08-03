package az.flowix.organization.repository;

import az.flowix.organization.entity.Organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    List<Organization> findAllByDeletedFalseOrderByCreatedAtDesc();

    Optional<Organization> findByIdAndDeletedFalse(UUID id);

    boolean existsBySlugAndDeletedFalse(String slug);

}
