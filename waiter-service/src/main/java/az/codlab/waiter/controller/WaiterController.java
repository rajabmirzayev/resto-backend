package az.codlab.waiter.controller;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.common.security.model.UserPrincipal;
import az.codlab.waiter.dto.WaiterOrderResponse;
import az.codlab.waiter.dto.WaiterTablesWrapper;
import az.codlab.waiter.service.WaiterService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class WaiterController {

    private final WaiterService waiterService;

    public WaiterController(WaiterService waiterService) {
        this.waiterService = waiterService;
    }

    @GetMapping("/tables")
    public ResponseEntity<ApiResponse<WaiterTablesWrapper>> getTables(
            @RequestParam UUID orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(waiterService.getTables(orgId, principal)));
    }

    @GetMapping("/orders/pending-confirm")
    public ResponseEntity<ApiResponse<List<WaiterOrderResponse>>> getPendingConfirmOrders(
            @RequestParam UUID orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(waiterService.getPendingConfirmOrders(orgId, principal)));
    }

    @GetMapping("/orders/payment-requests")
    public ResponseEntity<ApiResponse<List<WaiterOrderResponse>>> getPaymentRequests(
            @RequestParam UUID orgId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(waiterService.getPaymentRequests(orgId, principal)));
    }

}
