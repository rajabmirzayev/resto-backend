package az.codlab.table.repository;

import az.codlab.table.entity.Section;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SectionRepository extends JpaRepository<Section, UUID> {

    List<Section> findAllByOrgIdAndDeletedFalseOrderByCreatedAtAsc(UUID orgId);

    Optional<Section> findByIdAndDeletedFalse(UUID id);

    long countByOrgIdAndDeletedFalse(UUID orgId);

}
