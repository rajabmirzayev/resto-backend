package az.flowix.access.repository;

import az.flowix.access.entity.UiGroup;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UiGroupRepository extends JpaRepository<UiGroup, UUID> {

    Optional<UiGroup> findByCodeAndDeletedFalse(String code);

    Optional<UiGroup> findByIdAndDeletedFalse(UUID id);

    List<UiGroup> findAllByIdIn(Collection<UUID> ids);

    List<UiGroup> findAllByDeletedFalseAndIsActiveTrue();

    List<UiGroup> findAllByModuleIdAndDeletedFalseAndIsActiveTrue(UUID moduleId);

}
