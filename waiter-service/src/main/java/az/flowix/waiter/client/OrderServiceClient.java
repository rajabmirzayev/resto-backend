package az.flowix.waiter.client;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.waiter.client.dto.OrderServiceOrderResponse;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${service.order.url}")
public interface OrderServiceClient {

    @GetMapping("/api/order-ms/v1/internal/orders")
    ApiResponse<List<OrderServiceOrderResponse>> getOrders(
            @RequestParam UUID orgId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID waiterId);

}
