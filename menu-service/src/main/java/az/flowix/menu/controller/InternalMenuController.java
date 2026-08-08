package az.flowix.menu.controller;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.menu.dto.CategoryResponse;
import az.flowix.menu.dto.MenuItemResponse;
import az.flowix.menu.service.MenuService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/internal")
public class InternalMenuController {

    private final MenuService menuService;

    public InternalMenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getAllCategories(orgId, null)));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<MenuItemResponse>>> getItems(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getAllItems(orgId, null, null, null)));
    }

}
