package az.flowix.organization.client;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.organization.client.dto.ClientOrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "order-service", url = "${service.order.url}")
public interface OrderServiceClient {

    @GetMapping("/api/order-ms/v1/orders")
    ApiResponse<List<ClientOrderResponse>> getOrders(@RequestParam("orgId") UUID orgId);
}
