package az.codlab.order.client;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.order.client.dto.ClientMenuItemResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "menu-service", url = "${service.menu.url}")
public interface MenuServiceClient {

    @GetMapping("/api/menu-ms/v1/items")
    ApiResponse<List<ClientMenuItemResponse>> getItems(@RequestParam("orgId") UUID orgId);
}
