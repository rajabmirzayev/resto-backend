package az.codlab.kitchen.client;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.kitchen.client.dto.OrderServiceOrderResponse;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${service.order.url}")
public interface OrderServiceClient {

    @GetMapping("/api/order-ms/v1/orders")
    ApiResponse<List<OrderServiceOrderResponse>> getOrders(@RequestParam UUID orgId);

}
