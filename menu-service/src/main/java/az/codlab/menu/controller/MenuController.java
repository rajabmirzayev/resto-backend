package az.codlab.menu.controller;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.common.security.model.UserPrincipal;
import az.codlab.menu.dto.CategoryDeleteRequest;
import az.codlab.menu.dto.CategoryRequest;
import az.codlab.menu.dto.CategoryResponse;
import az.codlab.menu.dto.CategoryUpdateRequest;
import az.codlab.menu.dto.ImageUploadResponse;
import az.codlab.menu.dto.MenuItemRequest;
import az.codlab.menu.dto.MenuItemResponse;
import az.codlab.menu.dto.MenuItemUpdateRequest;
import az.codlab.menu.service.ImageStorageService;
import az.codlab.menu.service.MenuService;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1")
public class MenuController {

    private final MenuService menuService;

    private final ImageStorageService imageStorageService;

    public MenuController(MenuService menuService, ImageStorageService imageStorageService) {
        this.menuService = menuService;
        this.imageStorageService = imageStorageService;
    }

    // ======================== Categories ========================

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories(
            @RequestParam UUID orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var categories = menuService.getAllCategories(orgId, principal);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        var category = menuService.getCategoryById(id, principal);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var category = menuService.createCategory(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(category, "Category created"));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var category = menuService.updateCategory(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(category, "Category updated"));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable UUID id,
            @RequestBody(required = false) CategoryDeleteRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        menuService.deleteCategory(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted"));
    }

    // ======================== Menu Items ========================

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getAllItems(
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean available,
            @AuthenticationPrincipal UserPrincipal principal) {
        var items = menuService.getAllItems(orgId, categoryId, available, principal);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getItem(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        var item = menuService.getItemById(id, principal);
        return ResponseEntity.ok(ApiResponse.success(item));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<MenuItemResponse>> createItem(
            @Valid @RequestBody MenuItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var item = menuService.createItem(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(item, "Menu item created"));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody MenuItemUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        var item = menuService.updateItem(id, request, principal);
        return ResponseEntity.ok(ApiResponse.success(item, "Menu item updated"));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        menuService.deleteItem(id, principal);
        return ResponseEntity.ok(ApiResponse.success(null, "Menu item deleted"));
    }

    @PostMapping(value = "/items/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        menuService.getItemById(id, principal);
        var imageUrl = imageStorageService.storeImage(id, file);
        menuService.updateItemImage(id, imageUrl, principal);
        return ResponseEntity.ok(ApiResponse.success(
                new ImageUploadResponse(imageUrl), "Image uploaded"));
    }

    @DeleteMapping("/items/{id}/image")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        var item = menuService.getItemById(id, principal);
        if (item.getImageUrl() != null) {
            imageStorageService.deleteImage(item.getImageUrl());
        }
        menuService.deleteItemImage(id, principal);
        return ResponseEntity.ok(ApiResponse.success(null, "Image deleted"));
    }

}
