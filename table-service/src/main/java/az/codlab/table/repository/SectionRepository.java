package az.codlab.table.repository;

import az.codlab.table.entity.Section;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SectionRepository extends JpaRepository<Section, UUID> {

    List<Section> findAllByOrgIdAndDeletedFalseOrderByCreatedAtAsc(UUID orgId);

    Optional<Section> findByIdAndDeletedFalse(UUID id);

    long countByOrgIdAndDeletedFalse(UUID orgId);

    @Query("""
            select count(s) > 0 from Section s
            where s.orgId = :orgId
              and lower(s.name) = lower(:name)
              and s.deleted = false
              and s.id <> :excludeId
            """)
    boolean existsByOrgIdAndNameIgnoreCase(@Param("orgId") UUID orgId,
                                           @Param("name") String name,
                                           @Param("excludeId") UUID excludeId);

    @Query("""
            select count(s) > 0 from Section s
            where s.orgId = :orgId
              and lower(s.name) = lower(:name)
              and s.deleted = false
            """)
    boolean existsByOrgIdAndNameIgnoreCase(@Param("orgId") UUID orgId,
                                           @Param("name") String name);

}
