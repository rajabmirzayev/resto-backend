package az.flowix.kitchen.service;

import az.flowix.common.enums.OrderStatus;
import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.kitchen.client.OrderServiceClient;
import az.flowix.kitchen.client.dto.OrderServiceOrderResponse;
import az.flowix.kitchen.dto.KitchenOrderResponse;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class KitchenService {

    private static final Logger log = LoggerFactory.getLogger(KitchenService.class);

    private final OrderServiceClient orderServiceClient;

    public KitchenService(OrderServiceClient orderServiceClient) {
        this.orderServiceClient = orderServiceClient;
    }

    public KitchenOrderGroup getOrders(UUID orgId) {
        log.info("Fetching kitchen orders for org: {}", orgId);

        var pending = fetchByStatus(orgId, OrderStatus.PENDING.name());
        var confirmed = fetchByStatus(orgId, OrderStatus.CONFIRMED.name());
        var preparing = fetchByStatus(orgId, OrderStatus.PREPARING.name());
        var ready = fetchByStatus(orgId, OrderStatus.READY.name());

        var newOrders = Stream.concat(pending.stream(), confirmed.stream())
                .map(this::toKitchenResponse)
                .toList();

        var preparingList = preparing.stream()
                .map(this::toKitchenResponse)
                .toList();

        var readyList = ready.stream()
                .map(this::toKitchenResponse)
                .toList();

        return new KitchenOrderGroup(newOrders, preparingList, readyList);
    }

    private List<OrderServiceOrderResponse> fetchByStatus(UUID orgId, String status) {
        var response = orderServiceClient.getOrders(orgId, status);
        return unwrap(response);
    }

    private KitchenOrderResponse toKitchenResponse(OrderServiceOrderResponse o) {
        List<KitchenOrderResponse.KitchenItemResponse> items;
        if (o.getItems() != null) {
            items = o.getItems().stream()
                    .filter(i -> i != null)
                    .map(i -> KitchenOrderResponse.KitchenItemResponse.builder()
                            .id(i.getId())
                            .menuItemId(i.getMenuItemId())
                            .menuItemName(i.getMenuItemName())
                            .quantity(i.getQuantity())
                            .price(i.getPrice())
                            .notes(i.getNotes())
                            .status(i.getStatus())
                            .build())
                    .toList();
        } else {
            items = List.of();
        }

        return KitchenOrderResponse.builder()
                .id(o.getId())
                .items(items)
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

    private static <T> List<T> unwrap(ApiResponse<List<T>> response) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new RuntimeException("Order service returned unsuccessful response");
        }
        return response.getData();
    }

    public record KitchenOrderGroup(
            List<KitchenOrderResponse> newOrders,
            List<KitchenOrderResponse> preparing,
            List<KitchenOrderResponse> ready
    ) {}

}
