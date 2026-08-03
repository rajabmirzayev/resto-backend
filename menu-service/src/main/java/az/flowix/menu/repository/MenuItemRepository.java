package az.flowix.menu.repository;

import az.flowix.menu.entity.MenuItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findAllByOrgIdAndDeletedFalseOrderByCreatedAtDesc(UUID orgId);

    List<MenuItem> findAllByOrgIdAndCategoryIdAndDeletedFalseOrderByCreatedAtDesc(UUID orgId, UUID categoryId);

    List<MenuItem> findAllByOrgIdAndIsAvailableAndDeletedFalseOrderByCreatedAtDesc(UUID orgId, boolean isAvailable);

    List<MenuItem> findAllByOrgIdAndCategoryIdAndIsAvailableAndDeletedFalseOrderByCreatedAtDesc(
            UUID orgId, UUID categoryId, boolean isAvailable);

    Optional<MenuItem> findByIdAndDeletedFalse(UUID id);

    List<MenuItem> findAllByCategoryIdAndDeletedFalse(UUID categoryId);

}
