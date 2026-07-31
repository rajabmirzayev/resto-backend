package az.codlab.waiter.service;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.waiter.client.OrderServiceClient;
import az.codlab.waiter.client.TableServiceClient;
import az.codlab.waiter.client.dto.OrderServiceOrderResponse;
import az.codlab.waiter.client.dto.TableServiceSectionResponse;
import az.codlab.waiter.client.dto.TableServiceTableResponse;
import az.codlab.waiter.dto.WaiterOrderResponse;
import az.codlab.waiter.dto.WaiterTableResponse;
import az.codlab.waiter.dto.WaiterTablesWrapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WaiterService {

    private static final Logger log = LoggerFactory.getLogger(WaiterService.class);

    private final OrderServiceClient orderServiceClient;
    private final TableServiceClient tableServiceClient;

    public WaiterService(OrderServiceClient orderServiceClient, TableServiceClient tableServiceClient) {
        this.orderServiceClient = orderServiceClient;
        this.tableServiceClient = tableServiceClient;
    }

    public WaiterTablesWrapper getTables(UUID orgId) {
        log.debug("Fetching waiter tables for org: {}", orgId);
        var tables = unwrapList(tableServiceClient.getTables(orgId));
        var sections = unwrapList(tableServiceClient.getSections(orgId));
        var sectionNames = sections.stream()
                .collect(Collectors.toMap(TableServiceSectionResponse::getId, TableServiceSectionResponse::getName));
        var orders = unwrapList(orderServiceClient.getOrders(orgId, null, null));
        var orderMap = orders.stream()
                .collect(Collectors.toMap(
                        o -> o.getTableId(),
                        o -> o,
                        (a, b) -> a));

        var tableResponses = tables.stream()
                .map(t -> {
                    var summary = orderMap.containsKey(t.getId())
                            ? WaiterTableResponse.OrderSummary.builder()
                                    .totalAmount(orderMap.get(t.getId()).getTotalAmount())
                                    .itemCount(orderMap.get(t.getId()).getItems().size())
                                    .status(orderMap.get(t.getId()).getStatus())
                                    .build()
                            : null;
                    return WaiterTableResponse.builder()
                            .id(t.getId())
                            .tableNumber(t.getTableNumber())
                            .capacity(t.getCapacity())
                            .status(t.getStatus())
                            .section(sectionNames.getOrDefault(t.getSectionId(), ""))
                            .currentOrderId(t.getCurrentOrderId())
                            .orderSummary(summary)
                            .build();
                })
                .toList();

        return new WaiterTablesWrapper(tableResponses);
    }

    public List<WaiterOrderResponse> getPendingConfirmOrders(UUID orgId) {
        log.debug("Fetching pending confirm orders for org: {}", orgId);
        var orders = unwrapList(orderServiceClient.getOrders(orgId, "PENDING", null));
        return orders.stream()
                .filter(o -> !o.isWaiterConfirmed() && "CUSTOMER".equals(o.getOrderSource()))
                .map(this::toWaiterOrderResponse)
                .toList();
    }

    public List<WaiterOrderResponse> getPaymentRequests(UUID orgId) {
        log.debug("Fetching payment requests for org: {}", orgId);
        var allOrders = unwrapList(orderServiceClient.getOrders(orgId, null, null));
        return allOrders.stream()
                .filter(o -> o.isPaymentRequested() && "PENDING".equals(o.getPaymentStatus()))
                .map(this::toWaiterOrderResponse)
                .toList();
    }

    private WaiterOrderResponse toWaiterOrderResponse(OrderServiceOrderResponse o) {
        return WaiterOrderResponse.builder()
                .id(o.getId())
                .items(o.getItems().stream()
                        .map(i -> WaiterOrderResponse.ItemResponse.builder()
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
                .waiterId(o.getWaiterId())
                .waiterName(o.getWaiterName())
                .orderSource(o.getOrderSource())
                .waiterConfirmed(o.isWaiterConfirmed())
                .confirmedBy(o.getConfirmedBy())
                .customerPhoto(o.getCustomerPhoto())
                .paymentMethod(o.getPaymentMethod())
                .paymentRequested(o.isPaymentRequested())
                .cancelReason(o.getCancelReason())
                .orgId(o.getOrgId())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    private static <T> List<T> unwrapList(ApiResponse<List<T>> response) {
        return response != null && response.isSuccess() && response.getData() != null
                ? response.getData() : List.of();
    }

}
