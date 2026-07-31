package az.codlab.customer.client;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.customer.client.dto.MenuServiceCategoryResponse;
import az.codlab.customer.client.dto.MenuServiceItemResponse;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "menu-service", url = "${service.menu.url}")
public interface MenuServiceClient {

    @GetMapping("/api/menu-ms/v1/categories")
    ApiResponse<List<MenuServiceCategoryResponse>> getCategories(@RequestParam UUID orgId);

    @GetMapping("/api/menu-ms/v1/items")
    ApiResponse<List<MenuServiceItemResponse>> getItems(@RequestParam UUID orgId);

}
