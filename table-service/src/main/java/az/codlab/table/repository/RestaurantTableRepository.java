package az.codlab.table.repository;

import az.codlab.common.enums.TableStatus;
import az.codlab.table.entity.RestaurantTable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, UUID> {

    List<RestaurantTable> findAllByOrgIdAndDeletedFalseOrderByTableNumberAsc(UUID orgId);

    List<RestaurantTable> findAllByOrgIdAndSectionIdAndDeletedFalseOrderByTableNumberAsc(
            UUID orgId, UUID sectionId);

    List<RestaurantTable> findAllByOrgIdAndStatusAndDeletedFalseOrderByTableNumberAsc(
            UUID orgId, TableStatus status);

    Optional<RestaurantTable> findByIdAndDeletedFalse(UUID id);

    List<RestaurantTable> findAllBySectionIdAndDeletedFalse(UUID sectionId);

}
