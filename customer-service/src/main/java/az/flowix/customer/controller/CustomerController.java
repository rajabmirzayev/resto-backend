package az.flowix.customer.controller;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.customer.dto.BillRequest;
import az.flowix.customer.dto.CustomerMenuResponse;
import az.flowix.customer.dto.CustomerOrderRequest;
import az.flowix.customer.dto.CustomerOrderResponse;
import az.flowix.customer.dto.CustomerTableResponse;
import az.flowix.customer.service.CustomerService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
@RequestMapping("/v1")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/{orgId}/menu")
    public ResponseEntity<ApiResponse<CustomerMenuResponse>> getMenu(@PathVariable UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getMenu(orgId)));
    }

    @GetMapping("/{orgId}/tables")
    public ResponseEntity<ApiResponse<List<CustomerTableResponse>>> getTables(
            @PathVariable UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getAvailableTables(orgId)));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<CustomerOrderResponse>> createOrder(
            @Valid @RequestBody CustomerOrderRequest request) {
        var order = customerService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order placed"));
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<CustomerOrderResponse>> getOrder(
            @PathVariable UUID orderId,
            @RequestParam(required = false) String token) {
        var order = customerService.getOrder(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        if (token == null || order.getAccessToken() == null
                || !MessageDigest.isEqual(
                    token.getBytes(StandardCharsets.UTF_8),
                    order.getAccessToken().getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/orders/{orderId}/request-bill")
    public ResponseEntity<ApiResponse<Void>> requestBill(
            @PathVariable UUID orderId,
            @RequestParam(required = false) String token,
            @Valid @RequestBody BillRequest request) {
        var order = customerService.getOrder(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        if (token == null || order.getAccessToken() == null
                || !MessageDigest.isEqual(
                    token.getBytes(StandardCharsets.UTF_8),
                    order.getAccessToken().getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        customerService.requestBill(orderId, request.getMethod());
        return ResponseEntity.ok(ApiResponse.success(null, "Bill requested"));
    }

}
