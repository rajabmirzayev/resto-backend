package az.flowix.kitchen.controller;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.kitchen.service.KitchenService;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/orders")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<KitchenService.KitchenOrderGroup>> getOrders(
            @RequestParam UUID orgId) {
        var orders = kitchenService.getOrders(orgId);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

}
