package az.flowix.table.repository;

import az.flowix.common.enums.TableStatus;
import az.flowix.table.entity.RestaurantTable;

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

    List<RestaurantTable> findAllByOrgIdAndSectionIdAndStatusAndDeletedFalseOrderByTableNumberAsc(
            UUID orgId, UUID sectionId, TableStatus status);

    Optional<RestaurantTable> findByIdAndDeletedFalse(UUID id);

    boolean existsByOrgIdAndTableNumberAndDeletedFalse(UUID orgId, Integer tableNumber);

    boolean existsByOrgIdAndTableNumberAndIdNotAndDeletedFalse(
            UUID orgId, Integer tableNumber, UUID id);

    List<RestaurantTable> findAllBySectionIdAndDeletedFalse(UUID sectionId);

}
