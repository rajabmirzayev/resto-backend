package az.codlab.menu.controller;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.menu.dto.CategoryDeleteRequest;
import az.codlab.menu.dto.CategoryRequest;
import az.codlab.menu.dto.CategoryResponse;
import az.codlab.menu.dto.CategoryUpdateRequest;
import az.codlab.menu.dto.ImageUploadResponse;
import az.codlab.menu.dto.MenuItemRequest;
import az.codlab.menu.dto.MenuItemResponse;
import az.codlab.menu.dto.MenuItemUpdateRequest;
import az.codlab.menu.service.MenuService;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg", "image/png", "image/webp");

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    // ======================== Categories ========================

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories(
            @RequestParam UUID orgId) {
        var categories = menuService.getAllCategories(orgId);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(@PathVariable UUID id) {
        var category = menuService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        var category = menuService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(category, "Category created"));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryUpdateRequest request) {
        var category = menuService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(category, "Category updated"));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable UUID id,
            @RequestBody(required = false) CategoryDeleteRequest request) {
        menuService.deleteCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted"));
    }

    // ======================== Menu Items ========================

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getAllItems(
            @RequestParam(required = false) UUID orgId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean available) {
        var items = menuService.getAllItems(orgId, categoryId, available);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getItem(@PathVariable UUID id) {
        var item = menuService.getItemById(id);
        return ResponseEntity.ok(ApiResponse.success(item));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<MenuItemResponse>> createItem(
            @Valid @RequestBody MenuItemRequest request) {
        var item = menuService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(item, "Menu item created"));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateItem(
            @PathVariable UUID id,
            @Valid @RequestBody MenuItemUpdateRequest request) {
        var item = menuService.updateItem(id, request);
        return ResponseEntity.ok(ApiResponse.success(item, "Menu item updated"));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable UUID id) {
        menuService.deleteItem(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Menu item deleted"));
    }

    @PostMapping(value = "/items/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImageUploadResponse>> uploadImage(
            @PathVariable UUID id,
            @RequestPart("file") MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 2MB limit");
        }
        if (!ALLOWED_MIME_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only JPEG, PNG and WebP images are allowed");
        }
        // TODO: real file storage (S3/CDN) ile evez et
        var imageUrl = "https://cdn.tabler.az/images/" + id + ".jpg";
        menuService.updateItemImage(id, imageUrl);
        return ResponseEntity.ok(ApiResponse.success(
                new ImageUploadResponse(imageUrl), "Image uploaded"));
    }

    @DeleteMapping("/items/{id}/image")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable UUID id) {
        menuService.deleteItemImage(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Image deleted"));
    }

}
