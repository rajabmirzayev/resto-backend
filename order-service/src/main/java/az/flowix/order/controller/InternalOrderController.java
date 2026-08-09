package az.flowix.order.controller;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.order.dto.OrderRequest;
import az.flowix.order.dto.OrderResponse;
import az.flowix.order.dto.PaymentRequest;
import az.flowix.order.service.OrderService;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/internal")
public class InternalOrderController {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @RequestParam UUID orgId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID tableId,
            @RequestParam(required = false) UUID waiterId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrders(orgId, status, tableId, waiterId)));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID id,
            @RequestParam(required = false) UUID orgId) {
        var order = orderService.getOrder(id);
        if (orgId != null && !orgId.equals(order.getOrgId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        var order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(order, "Order created"));
    }

    @PostMapping("/orders/{id}/request-payment")
    public ResponseEntity<ApiResponse<OrderResponse>> requestPayment(
            @PathVariable UUID id, @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.requestPayment(id, request)));
    }

}
