package az.flowix.waiter.service;

import az.flowix.common.enums.OrderSource;
import az.flowix.common.enums.OrderStatus;
import az.flowix.common.enums.PaymentStatus;
import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.common.security.model.UserPrincipal;
import az.flowix.waiter.client.OrderServiceClient;
import az.flowix.waiter.client.TableServiceClient;
import az.flowix.waiter.client.dto.OrderServiceOrderResponse;
import az.flowix.waiter.client.dto.TableServiceSectionResponse;
import az.flowix.waiter.client.dto.TableServiceTableResponse;
import az.flowix.waiter.dto.WaiterOrderResponse;
import az.flowix.waiter.dto.WaiterTableResponse;
import az.flowix.waiter.dto.WaiterTablesWrapper;
import az.flowix.waiter.error.WaiterErrorCode;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WaiterService {

    private static final Logger log = LoggerFactory.getLogger(WaiterService.class);

    private static final int MAX_RESULTS = 200;

    private final OrderServiceClient orderServiceClient;
    private final TableServiceClient tableServiceClient;

    public WaiterService(OrderServiceClient orderServiceClient, TableServiceClient tableServiceClient) {
        this.orderServiceClient = orderServiceClient;
        this.tableServiceClient = tableServiceClient;
    }

    public WaiterTablesWrapper getTables(UUID orgId, UserPrincipal principal) {
        assertCanReadOrg(orgId, principal);
        log.debug("Fetching waiter tables for org: {}", orgId);

        var tables = unwrapList(tableServiceClient.getTables(orgId), "table-service.tables");
        var sections = unwrapList(tableServiceClient.getSections(orgId), "table-service.sections");
        var orders = unwrapList(orderServiceClient.getOrders(orgId, null, null), "order-service.orders");

        Map<String, TableServiceSectionResponse> sectionsById = sections.stream()
                .filter(s -> s != null && s.getId() != null)
                .collect(Collectors.toMap(
                        s -> s.getId().toString(), s -> s, (a, b) -> a));

        Map<String, OrderServiceOrderResponse> ordersById = orders.stream()
                .filter(o -> o != null && o.getId() != null)
                .collect(Collectors.toMap(
                        OrderServiceOrderResponse::getId, o -> o, (a, b) -> a));

        var tableResponses = tables.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TableServiceTableResponse::getTableNumber,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(t -> {
                    WaiterTableResponse.OrderSummary summary = null;
                    if (t.getCurrentOrderId() != null) {
                        var order = ordersById.get(t.getCurrentOrderId().toString());
                        if (order != null) {
                            summary = WaiterTableResponse.OrderSummary.builder()
                                    .totalAmount(order.getTotalAmount() != null
                                            ? order.getTotalAmount() : BigDecimal.ZERO)
                                    .itemCount(order.getItems() != null ? order.getItems().size() : 0)
                                    .status(order.getStatus() != null ? order.getStatus() : "")
                                    .build();
                        }
                    }
                    var section = t.getSectionId() != null
                            ? sectionsById.get(t.getSectionId().toString()) : null;
                    return WaiterTableResponse.builder()
                            .id(t.getId())
                            .tableNumber(t.getTableNumber())
                            .capacity(t.getCapacity())
                            .status(t.getStatus())
                            .section(section != null ? section.getName() : "")
                            .currentOrderId(t.getCurrentOrderId())
                            .orderSummary(summary)
                            .build();
                })
                .toList();

        return new WaiterTablesWrapper(tableResponses);
    }

    public List<WaiterOrderResponse> getPendingConfirmOrders(UUID orgId, UserPrincipal principal) {
        assertCanReadOrg(orgId, principal);
        log.debug("Fetching pending confirm orders for org: {}", orgId);

        var orders = unwrapList(orderServiceClient.getOrders(orgId, OrderStatus.PENDING.name(), null),
                "order-service.orders");
        return orders.stream()
                .filter(Objects::nonNull)
                .filter(o -> !o.isWaiterConfirmed())
                .filter(o -> OrderSource.CUSTOMER.name().equalsIgnoreCase(o.getOrderSource()))
                .sorted(createdAtDescending())
                .limit(MAX_RESULTS)
                .map(this::toWaiterOrderResponse)
                .toList();
    }

    public List<WaiterOrderResponse> getPaymentRequests(UUID orgId, UserPrincipal principal) {
        assertCanReadOrg(orgId, principal);
        log.debug("Fetching payment requests for org: {}", orgId);

        var allOrders = unwrapList(orderServiceClient.getOrders(orgId, null, null), "order-service.orders");
        return allOrders.stream()
                .filter(Objects::nonNull)
                .filter(OrderServiceOrderResponse::isPaymentRequested)
                .filter(o -> PaymentStatus.PENDING.name().equalsIgnoreCase(o.getPaymentStatus()))
                .sorted(createdAtDescending())
                .limit(MAX_RESULTS)
                .map(this::toWaiterOrderResponse)
                .toList();
    }

    private void assertCanReadOrg(UUID orgId, UserPrincipal principal) {
        if (orgId == null || principal == null) {
            return;
        }
        if (principal.getUserId() != null
                && !principal.isPlatformAdmin()
                && (principal.getOrgId() == null || !principal.getOrgId().equals(orgId.toString()))) {
            throw WaiterErrorCode.ACCESS_DENIED.forbidden();
        }
    }

    private static Comparator<OrderServiceOrderResponse> createdAtDescending() {
        return Comparator.comparing(OrderServiceOrderResponse::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private WaiterOrderResponse toWaiterOrderResponse(OrderServiceOrderResponse o) {
        List<WaiterOrderResponse.ItemResponse> items = o.getItems() == null ? List.of() : o.getItems().stream()
                .filter(Objects::nonNull)
                .map(i -> WaiterOrderResponse.ItemResponse.builder()
                        .id(i.getId())
                        .menuItemId(i.getMenuItemId())
                        .menuItemName(i.getMenuItemName())
                        .quantity(i.getQuantity())
                        .price(i.getPrice())
                        .notes(i.getNotes())
                        .status(i.getStatus())
                        .build())
                .toList();
        return WaiterOrderResponse.builder()
                .id(o.getId())
                .items(items)
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

    private static <T> List<T> unwrapList(ApiResponse<List<T>> response, String source) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw WaiterErrorCode.UPSTREAM_ERROR.exceptionWithMessage(HttpStatus.BAD_GATEWAY,
                    "Upstream service '" + source + "' returned an invalid response");
        }
        return response.getData();
    }

}
