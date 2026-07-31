package az.codlab.kitchen.service;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.kitchen.client.OrderServiceClient;
import az.codlab.kitchen.dto.KitchenOrderResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KitchenService {

    private static final Logger log = LoggerFactory.getLogger(KitchenService.class);

    private static final Set<String> NEW_STATUSES = Set.of("PENDING", "CONFIRMED");
    private static final Set<String> PREPARING_STATUSES = Set.of("PREPARING");
    private static final Set<String> READY_STATUSES = Set.of("READY");

    private final OrderServiceClient orderServiceClient;

    public KitchenService(OrderServiceClient orderServiceClient) {
        this.orderServiceClient = orderServiceClient;
    }

    public KitchenOrderGroup getOrders(UUID orgId) {
        log.debug("Fetching kitchen orders for org: {}", orgId);
        var response = orderServiceClient.getOrders(orgId);
        var allOrders = response != null && response.isSuccess() && response.getData() != null
                ? response.getData() : List.<az.codlab.kitchen.client.dto.OrderServiceOrderResponse>of();

        var newOrders = allOrders.stream()
                .filter(o -> NEW_STATUSES.contains(o.getStatus()))
                .map(this::toKitchenResponse)
                .toList();

        var preparing = allOrders.stream()
                .filter(o -> PREPARING_STATUSES.contains(o.getStatus()))
                .map(this::toKitchenResponse)
                .toList();

        var ready = allOrders.stream()
                .filter(o -> READY_STATUSES.contains(o.getStatus()))
                .map(this::toKitchenResponse)
                .toList();

        return new KitchenOrderGroup(newOrders, preparing, ready);
    }

    private KitchenOrderResponse toKitchenResponse(az.codlab.kitchen.client.dto.OrderServiceOrderResponse o) {
        return KitchenOrderResponse.builder()
                .id(o.getId())
                .items(o.getItems().stream()
                        .map(i -> KitchenOrderResponse.KitchenItemResponse.builder()
                                .id(i.getId())
                                .menuItemId(i.getMenuItemId())
                                .menuItemName(i.getMenuItemName())
                                .quantity(i.getQuantity())
                                .price(i.getPrice())
                                .notes(i.getNotes())
                                .status(i.getStatus())
                                .build())
                        .toList())
                .tableId(o.getTableId())
                .tableNumber(o.getTableNumber())
                .status(o.getStatus())
                .paymentStatus(o.getPaymentStatus())
                .totalAmount(o.getTotalAmount())
                .waiterName(o.getWaiterName())
                .orderSource(o.getOrderSource())
                .createdAt(o.getCreatedAt())
                .build();
    }

    public record KitchenOrderGroup(
            List<KitchenOrderResponse> newOrders,
            List<KitchenOrderResponse> preparing,
            List<KitchenOrderResponse> ready
    ) {}

}
