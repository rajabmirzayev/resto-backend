package az.flowix.organization.repository;

import az.flowix.organization.entity.LocalSection;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocalSectionRepository extends JpaRepository<LocalSection, UUID> {

    List<LocalSection> findAllByOrgIdAndDeletedFalseOrderByNameAsc(UUID orgId);

}
