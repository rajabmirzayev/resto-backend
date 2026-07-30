package az.codlab.menu.repository;

import az.codlab.menu.entity.MenuCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategory, UUID> {

    List<MenuCategory> findAllByOrgIdAndDeletedFalseOrderBySortOrderAsc(UUID orgId);

    Optional<MenuCategory> findByIdAndDeletedFalse(UUID id);

}
