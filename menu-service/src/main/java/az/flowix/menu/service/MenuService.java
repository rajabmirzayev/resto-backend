package az.flowix.menu.service;

import az.flowix.common.security.model.UserPrincipal;
import az.flowix.menu.dto.CategoryDeleteRequest;
import az.flowix.menu.dto.CategoryRequest;
import az.flowix.menu.dto.CategoryResponse;
import az.flowix.menu.dto.CategoryUpdateRequest;
import az.flowix.menu.dto.MenuItemRequest;
import az.flowix.menu.dto.MenuItemResponse;
import az.flowix.menu.dto.MenuItemUpdateRequest;
import az.flowix.menu.entity.MenuCategory;
import az.flowix.menu.entity.MenuItem;
import az.flowix.menu.error.MenuErrorCode;
import az.flowix.menu.mapper.MenuCategoryMapper;
import az.flowix.menu.mapper.MenuItemMapper;
import az.flowix.menu.repository.MenuCategoryRepository;
import az.flowix.menu.repository.MenuItemRepository;

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

    public List<CategoryResponse> getAllCategories(UUID orgId, UserPrincipal principal) {
        assertCanReadOrg(orgId, principal);
        return menuCategoryMapper.toDtoList(
                menuCategoryRepository.findAllByOrgIdAndDeletedFalseOrderBySortOrderAsc(orgId));
    }

    public CategoryResponse getCategoryById(UUID id, UserPrincipal principal) {
        var category = menuCategoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(MenuErrorCode.CATEGORY_NOT_FOUND::notFound);
        assertCanReadOrg(category.getOrgId(), principal);
        return menuCategoryMapper.toDto(category);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, UserPrincipal principal) {
        var orgId = resolveOrgForCreate(principal, request.getOrgId());
        var category = MenuCategory.builder()
                .name(request.getName() != null ? request.getName().normalized() : null)
                .icon(normalizeIcon(request.getIcon()))
                .sortOrder(request.getSortOrder())
                .orgId(orgId)
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
            category.setIcon(normalizeIcon(request.getIcon()));
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
        if (!items.isEmpty()) {
            if (request.getMoveItemsTo() == null) {
                throw MenuErrorCode.CATEGORY_HAS_ITEMS.badRequest();
            }
            if (request.getMoveItemsTo().equals(id)) {
                throw MenuErrorCode.CATEGORY_SELF_MOVE.badRequest();
            }
            var target = menuCategoryRepository.findByIdAndDeletedFalse(request.getMoveItemsTo())
                    .orElseThrow(MenuErrorCode.CATEGORY_NOT_FOUND::notFound);
            assertOrgAccess(target.getOrgId(), principal);
            for (var item : items) {
                item.setCategoryId(request.getMoveItemsTo());
            }
            menuItemRepository.saveAll(items);
            log.info("Moved {} items to category {}", items.size(), request.getMoveItemsTo());
        }

        category.softDelete(userId(principal));
        menuCategoryRepository.save(category);
        log.info("Category soft-deleted: {}", id);
    }

    // ======================== Menu Items ========================

    public List<MenuItemResponse> getAllItems(UUID orgId, UUID categoryId, Boolean available,
                                              UserPrincipal principal) {
        assertCanReadOrg(orgId, principal);
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

    public MenuItemResponse getItemById(UUID id, UserPrincipal principal) {
        var item = menuItemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(MenuErrorCode.ITEM_NOT_FOUND::notFound);
        assertCanReadOrg(item.getOrgId(), principal);
        return menuItemMapper.toDto(item);
    }

    @Transactional
    public MenuItemResponse createItem(MenuItemRequest request, UserPrincipal principal) {
        var orgId = resolveOrgForCreate(principal, request.getOrgId());
        assertCategoryOwnedByOrg(request.getCategoryId(), orgId);
        var item = MenuItem.builder()
                .name(request.getName() != null ? request.getName().normalized() : null)
                .description(request.getDescription() != null ? request.getDescription().normalized() : null)
                .price(request.getPrice())
                .categoryId(request.getCategoryId())
                .preparationTime(request.getPreparationTime())
                .isAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true)
                .imageUrl(normalizeImageUrl(request.getImageUrl()))
                .orgId(orgId)
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
            item.setImageUrl(normalizeImageUrl(request.getImageUrl()));
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
        item.softDelete(userId(principal));
        menuItemRepository.save(item);
        log.info("Menu item soft-deleted: {}", id);
    }

    @Transactional
    public void updateItemImage(UUID id, String imageUrl, UserPrincipal principal) {
        var item = menuItemRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(MenuErrorCode.ITEM_NOT_FOUND::notFound);
        assertOrgAccess(item.getOrgId(), principal);
        item.setImageUrl(normalizeImageUrl(imageUrl));
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

    private UUID resolveOrgForCreate(UserPrincipal principal, UUID requestedOrgId) {
        if (principal == null || principal.getUserId() == null) {
            throw MenuErrorCode.ACCESS_DENIED.forbidden();
        }
        if (principal.isPlatformAdmin()) {
            if (requestedOrgId == null) {
                throw MenuErrorCode.ACCESS_DENIED.forbidden();
            }
            return requestedOrgId;
        }
        if (principal.getOrgId() == null || !principal.getOrgId().equals(requestedOrgId.toString())) {
            throw MenuErrorCode.ACCESS_DENIED.forbidden();
        }
        return UUID.fromString(principal.getOrgId());
    }

    private String normalizeIcon(String icon) {
        if (icon == null) {
            return null;
        }
        String trimmed = icon.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null) {
            return null;
        }
        String trimmed = imageUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private UUID userId(UserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            return null;
        }
        try {
            return UUID.fromString(principal.getUserId());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void assertCanReadOrg(UUID orgId, UserPrincipal principal) {
        if (principal == null || principal.getUserId() == null) {
            throw MenuErrorCode.ACCESS_DENIED.forbidden();
        }
        if (orgId == null) {
            if (!principal.isPlatformAdmin()) {
                throw MenuErrorCode.ACCESS_DENIED.forbidden();
            }
            return;
        }
        if (!principal.isPlatformAdmin()
                && (principal.getOrgId() == null || !principal.getOrgId().equals(orgId.toString()))) {
            throw MenuErrorCode.ACCESS_DENIED.forbidden();
        }
    }

    private void assertCategoryOwnedByOrg(UUID categoryId, UUID orgId) {
        var category = menuCategoryRepository.findByIdAndDeletedFalse(categoryId)
                .orElseThrow(MenuErrorCode.CATEGORY_NOT_FOUND::notFound);
        if (!category.getOrgId().equals(orgId)) {
            throw MenuErrorCode.ACCESS_DENIED.forbidden();
        }
    }

}
