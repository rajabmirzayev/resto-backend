package az.codlab.order.controller;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.order.dto.AddItemsRequest;
import az.codlab.order.dto.CancelRequest;
import az.codlab.order.dto.OrderRequest;
import az.codlab.order.dto.OrderResponse;
import az.codlab.order.dto.PaymentRequest;
import az.codlab.order.dto.StatusRequest;
import az.codlab.order.dto.WaiterConfirmRequest;
import az.codlab.order.service.OrderService;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @RequestParam UUID orgId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID tableId,
            @RequestParam(required = false) UUID waiterId) {
        var orders = orderService.getOrders(orgId, status, tableId, waiterId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable UUID id) {
        var order = orderService.getOrder(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @Valid @RequestBody OrderRequest request) {
        var order = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order created"));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusRequest request) {
        var order = orderService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order status updated"));
    }

    @PutMapping("/{id}/items/{itemId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateItemStatus(
            @PathVariable UUID id,
            @PathVariable UUID itemId,
            @Valid @RequestBody StatusRequest request) {
        var order = orderService.updateItemStatus(id, itemId, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Item status updated"));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ApiResponse<OrderResponse>> addItems(
            @PathVariable UUID id,
            @Valid @RequestBody AddItemsRequest request) {
        var order = orderService.addItems(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Items added to order"));
    }

    @PutMapping("/{id}/waiter-confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> waiterConfirm(
            @PathVariable UUID id,
            @Valid @RequestBody WaiterConfirmRequest request) {
        var order = orderService.waiterConfirm(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order confirmed"));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelRequest request) {
        var order = orderService.cancelOrder(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Order cancelled"));
    }

    @PostMapping("/{id}/request-payment")
    public ResponseEntity<ApiResponse<OrderResponse>> requestPayment(
            @PathVariable UUID id,
            @Valid @RequestBody PaymentRequest request) {
        var order = orderService.requestPayment(id, request);
        return ResponseEntity.ok(ApiResponse.success(order, "Payment requested"));
    }

    @PostMapping("/{id}/complete-payment")
    public ResponseEntity<ApiResponse<OrderResponse>> completePayment(@PathVariable UUID id) {
        var order = orderService.completePayment(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Payment completed"));
    }

    @PostMapping("/{id}/start-preparing")
    public ResponseEntity<ApiResponse<OrderResponse>> startPreparing(@PathVariable UUID id) {
        var order = orderService.startPreparing(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order is now being prepared"));
    }

    @PostMapping("/{id}/mark-all-ready")
    public ResponseEntity<ApiResponse<OrderResponse>> markAllReady(@PathVariable UUID id) {
        var order = orderService.markAllReady(id);
        return ResponseEntity.ok(ApiResponse.success(order, "All items are ready"));
    }

}
