package az.flowix.access.repository;

import az.flowix.access.entity.Module;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuleRepository extends JpaRepository<Module, UUID> {

    Optional<Module> findByCodeAndDeletedFalse(String code);

    Optional<Module> findByIdAndDeletedFalse(UUID id);

    List<Module> findAllByIdIn(Collection<UUID> ids);

    List<Module> findAllByDeletedFalseAndIsActiveTrue();

}
