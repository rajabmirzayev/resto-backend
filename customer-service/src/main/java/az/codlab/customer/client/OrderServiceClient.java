package az.codlab.customer.client;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.customer.client.dto.OrderServiceOrderRequest;
import az.codlab.customer.client.dto.OrderServiceOrderResponse;

import java.util.Map;
import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "order-service", url = "${service.order.url}")
public interface OrderServiceClient {

    @GetMapping("/api/order-ms/v1/orders/{id}")
    ApiResponse<OrderServiceOrderResponse> getOrder(@PathVariable UUID id);

    @PostMapping("/api/order-ms/v1/orders")
    ApiResponse<OrderServiceOrderResponse> createOrder(@RequestBody OrderServiceOrderRequest request);

    @PostMapping("/api/order-ms/v1/orders/{id}/request-payment")
    ApiResponse<Void> requestPayment(@PathVariable UUID id, @RequestBody Map<String, String> body);

}
