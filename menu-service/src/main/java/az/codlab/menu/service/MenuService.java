package az.codlab.menu.service;

import az.codlab.common.security.model.UserPrincipal;
import az.codlab.menu.dto.CategoryDeleteRequest;
import az.codlab.menu.dto.CategoryRequest;
import az.codlab.menu.dto.CategoryResponse;
import az.codlab.menu.dto.CategoryUpdateRequest;
import az.codlab.menu.dto.MenuItemRequest;
import az.codlab.menu.dto.MenuItemResponse;
import az.codlab.menu.dto.MenuItemUpdateRequest;
import az.codlab.menu.entity.MenuCategory;
import az.codlab.menu.entity.MenuItem;
import az.codlab.menu.error.MenuErrorCode;
import az.codlab.menu.mapper.MenuCategoryMapper;
import az.codlab.menu.mapper.MenuItemMapper;
import az.codlab.menu.repository.MenuCategoryRepository;
import az.codlab.menu.repository.MenuItemRepository;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);

    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryMapper menuCategoryMapper;
    private final MenuItemMapper menuItemMapper;

    public MenuService(MenuCategoryRepository menuCategoryRepository,
                       MenuItemRepository menuItemRepository,
                       MenuCategoryMapper menuCategoryMapper,
                       MenuItemMapper menuItemMapper) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuCategoryMapper = menuCategoryMapper;
        this.menuItemMapper = menuItemMapper;
    }

    // ======================== Categories ========================

    public List<CategoryResponse> getAllCategories(UUID orgId) {
        return menuCategoryMapper.toDtoList(
                menuCategoryRepository.findAllByOrgIdAndDeletedFalseOrderBySortOrderAsc(orgId));
    }

    public CategoryResponse getCategoryById(UUID id) {
        return menuCategoryRepository.findByIdAndDeletedFalse(id)
                .map(menuCategoryMapper::toDto)
                .orElseThrow(MenuErrorCode.CATEGORY_NOT_FOUND::notFound);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, UserPrincipal principal) {
        assertOrgAccess(request.getOrgId(), principal);
        var category = MenuCategory.builder()
                .name(request.getName().normalized())
                .icon(request.getIcon())
                .sortOrder(request.getSortOrder())
                .orgId(request.getOrgId())
                .build();
        category = menuCategoryRepository.save(category);
        log.info("Category created: {} ({})", category.getName(), category.getId());
        return menuCategoryMapper.toDto(category);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryUpdateRequest request, UserPrincipal principal) {
        var category = menuCategoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(MenuErrorCode.CATEGORY_NOT_FOUND::notFound);
        assertOrgAccess(category.getOrgId(), principal);

        if (request.getName() != null) {
            category.setName(request.getName().normalized());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        category = menuCategoryRepository.save(category);
        log.info("Category updated: {} ({})", category.getName(), category.getId());
        return menuCategoryMapper.toDto(category);
    }

    @Transactional
    public void deleteCategory(UUID id, CategoryDeleteRequest request, UserPrincipal principal) {
        var category = menuCategoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(MenuErrorCode.CATEGORY_NOT_FOUND::notFound);
        assertOrgAccess(category.getOrgId(), principal);

        var items = menuItemRepository.findAllByCategoryIdAndDeletedFalse(id);
        if (request != null && request.getMoveItemsTo() != null) {
            var target = menuCategoryRepository.findByIdAndDeletedFalse(request.getMoveItemsTo())
                    .orElseThrow(MenuErrorCode.CATEGORY_NOT_FOUND::notFound);
            assertOrgAccess(target.getOrgId(), principal);
            for (var item : items) {
                item.setCategoryId(request.getMoveItemsTo());
                menuItemRepository.save(item);
            }
            log.info("Moved {} items to category {}", items.size(), request.getMoveItemsTo());
        } else {
            for (var item : items) {
                item.softDelete(null);
                menuItemRepository.save(item);
            }
            log.info("Deleted {} items in category {}", items.size(), id);
        }

        category.softDelete(null);
        menuCategoryRepository.save(category);
        log.info("Category soft-deleted: {}", id);
    }

    // ======================== Menu Items ========================

    public List<MenuItemResponse> getAllItems(UUID orgId, UUID categoryId, Boolean available) {
        if (orgId != null && categoryId != null && available != null) {
            return menuItemMapper.toDtoList(
                    menuItemRepository.findAllByOrgIdAndCategoryIdAndIsAvailableAndDeletedFalseOrderByCreatedAtDesc(
                            orgId, categoryId, available));
        }
        if (orgId != null && categoryId != null) {
            return menuItemMapper.toDtoList(
                    menuItemRepository.findAllByOrgIdAndCategoryIdAndDeletedFalseOrderByCreatedAtDesc(
                            orgId, categoryId));
        }
        if (orgId != null && available != null) {
            return menuItemMapper.toDtoList(
                    menuItemRepository.findAllByOrgIdAndIsAvailableAndDeletedFalseOrderByCreatedAtDesc(
                            orgId, available));
        }
        if (orgId != null) {
            return menuItemMapper.toDtoList(
                    menuItemRepository.findAllByOrgIdAndDeletedFalseOrderByCreatedAtDesc(orgId));
        }
        return List.of();
    }

    public MenuItemResponse getItemById(UUID id) {
        return menuItemRepository.findByIdAndDeletedFalse(id)
                .map(menuItemMapper::toDto)
                .orElseThrow(MenuErrorCode.ITEM_NOT_FOUND::notFound);
    }

    @Transactional
    public MenuItemResponse createItem(MenuItemRequest request, UserPrincipal principal) {
        assertOrgAccess(request.getOrgId(), principal);
        assertCategoryOwnedByOrg(request.getCategoryId(), request.getOrgId());
        var item = MenuItem.builder()
                .name(request.getName().normalized())
                .description(request.getDescription() != null ? request.getDescription().normalized() : null)
                .price(request.getPrice())
                .categoryId(request.getCategoryId())
                .preparationTime(request.getPreparationTime())
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .imageUrl(request.getImageUrl())
                .orgId(request.getOrgId())
                .build();
        item = menuItemRepository.save(item);
        log.info("Menu item created: {} ({})", item.getName(), item.getId());
        return menuItemMapper.toDto(item);
    }

    @Transactional
    public MenuItemResponse updateItem(UUID id, MenuItemUpdateRequest request, UserPrincipal principal) {
        var item = menuItemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(MenuErrorCode.ITEM_NOT_FOUND::notFound);
        assertOrgAccess(item.getOrgId(), principal);

        if (request.getName() != null) {
            item.setName(request.getName().normalized());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription().normalized());
        }
        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
        }
        if (request.getCategoryId() != null) {
            assertCategoryOwnedByOrg(request.getCategoryId(), item.getOrgId());
            item.setCategoryId(request.getCategoryId());
        }
        if (request.getPreparationTime() != null) {
            item.setPreparationTime(request.getPreparationTime());
        }
        if (request.getIsAvailable() != null) {
            item.setAvailable(request.getIsAvailable());
        }
        if (request.getImageUrl() != null) {
            item.setImageUrl(request.getImageUrl());
        }

        item = menuItemRepository.save(item);
        log.info("Menu item updated: {} ({})", item.getName(), item.getId());
        return menuItemMapper.toDto(item);
    }

    @Transactional
    public void deleteItem(UUID id, UserPrincipal principal) {
        var item = menuItemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(MenuErrorCode.ITEM_NOT_FOUND::notFound);
        assertOrgAccess(item.getOrgId(), principal);
        item.softDelete(null);
        menuItemRepository.save(item);
        log.info("Menu item soft-deleted: {}", id);
    }

    @Transactional
    public void updateItemImage(UUID id, String imageUrl, UserPrincipal principal) {
        var item = menuItemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(MenuErrorCode.ITEM_NOT_FOUND::notFound);
        assertOrgAccess(item.getOrgId(), principal);
        item.setImageUrl(imageUrl);
        menuItemRepository.save(item);
        log.info("Menu item image updated: {}", id);
    }

    @Transactional
    public void deleteItemImage(UUID id, UserPrincipal principal) {
        var item = menuItemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(MenuErrorCode.ITEM_NOT_FOUND::notFound);
        assertOrgAccess(item.getOrgId(), principal);
        item.setImageUrl(null);
        menuItemRepository.save(item);
        log.info("Menu item image deleted: {}", id);
    }

    private void assertOrgAccess(UUID orgId, UserPrincipal principal) {
        if (principal != null
                && (principal.isPlatformAdmin()
                    || (principal.getOrgId() != null && principal.getOrgId().equals(orgId.toString())))) {
            return;
        }
        throw MenuErrorCode.ACCESS_DENIED.forbidden();
    }

    private void assertCategoryOwnedByOrg(UUID categoryId, UUID orgId) {
        var category = menuCategoryRepository.findByIdAndDeletedFalse(categoryId)
                .orElseThrow(MenuErrorCode.CATEGORY_NOT_FOUND::notFound);
        if (!category.getOrgId().equals(orgId)) {
            throw MenuErrorCode.ACCESS_DENIED.forbidden();
        }
    }

}
