package az.flowix.dashboard.service;

import az.flowix.common.enums.OrderStatus;
import az.flowix.common.enums.TableStatus;
import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.common.type.LocalizedString;
import az.flowix.dashboard.client.MenuServiceClient;
import az.flowix.dashboard.client.OrderServiceClient;
import az.flowix.dashboard.client.TableServiceClient;
import az.flowix.dashboard.client.UserServiceClient;
import az.flowix.dashboard.dto.DashboardStatsResponse;
import az.flowix.dashboard.dto.RecentOrderResponse;
import az.flowix.dashboard.dto.StaffListResponse;
import az.flowix.dashboard.dto.TopItemResponse;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private final OrderServiceClient orderServiceClient;
    private final TableServiceClient tableServiceClient;
    private final MenuServiceClient menuServiceClient;
    private final UserServiceClient userServiceClient;

    public DashboardService(OrderServiceClient orderServiceClient, TableServiceClient tableServiceClient,
                            MenuServiceClient menuServiceClient, UserServiceClient userServiceClient) {
        this.orderServiceClient = orderServiceClient;
        this.tableServiceClient = tableServiceClient;
        this.menuServiceClient = menuServiceClient;
        this.userServiceClient = userServiceClient;
    }

    public DashboardStatsResponse getStats(UUID orgId) {
        log.info("Fetching dashboard stats for org: {}", orgId);
        var orders = unwrapList(orderServiceClient.getOrders(orgId), "order-service");
        var tables = unwrapList(tableServiceClient.getTables(orgId), "table-service");

        var completed = orders.stream()
                .filter(o -> isCompleted(o.getStatus()))
                .toList();
        var active = orders.stream()
                .filter(o -> isActive(o.getStatus()))
                .toList();
        var totalRevenue = completed.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var occupiedTables = (int) tables.stream()
                .filter(t -> TableStatus.OCCUPIED.name().equals(t.getStatus()))
                .count();

        return DashboardStatsResponse.builder()
                .totalRevenue(totalRevenue)
                .completedOrders(completed.size())
                .activeOrders(active.size())
                .occupiedTables(occupiedTables)
                .build();
    }

    public List<TopItemResponse> getTopItems(UUID orgId) {
        log.info("Fetching top items for org: {}", orgId);
        var orders = unwrapList(orderServiceClient.getOrders(orgId), "order-service");
        var items = unwrapList(menuServiceClient.getItems(orgId), "menu-service");
        var itemNames = items.stream()
                .collect(Collectors.toMap(
                        i -> i.getId(),
                        i -> i.getName() != null && i.getName().getEn() != null ? i.getName().getEn() : ""));

        var itemCounts = orders.stream()
                .flatMap(o -> o.getItems().stream())
                .collect(Collectors.groupingBy(
                        i -> i.getMenuItemId(),
                        Collectors.summingInt(i -> i.getQuantity() != null ? i.getQuantity() : 0)));

        return itemCounts.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> TopItemResponse.builder()
                        .menuItemId(e.getKey())
                        .name(new LocalizedString(null, itemNames.getOrDefault(e.getKey(), ""), null))
                        .count(e.getValue())
                        .build())
                .toList();
    }

    public List<RecentOrderResponse> getRecentOrders(UUID orgId) {
        log.info("Fetching recent orders for org: {}", orgId);
        return unwrapList(orderServiceClient.getOrders(orgId), "order-service").stream()
                .sorted(Comparator.comparing(o -> o.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .map(o -> RecentOrderResponse.builder()
                        .id(UUID.fromString(o.getId()))
                        .tableNumber(o.getTableNumber())
                        .waiterName(o.getWaiterName())
                        .totalAmount(o.getTotalAmount())
                        .status(o.getStatus())
                        .createdAt(o.getCreatedAt())
                        .build())
                .toList();
    }

    public List<StaffListResponse> getStaffList(UUID orgId) {
        log.info("Fetching staff list for org: {}", orgId);
        var users = unwrapPage(userServiceClient.getUsers(orgId, 100), "access-service");
        var orders = unwrapList(orderServiceClient.getOrders(orgId), "order-service");

        var activeOrdersByWaiter = orders.stream()
                .filter(o -> o.getWaiterId() != null && isActive(o.getStatus()))
                .collect(Collectors.groupingBy(o -> o.getWaiterId(), Collectors.counting()));

        return users.stream()
                .filter(u -> u.getRole() != null && !"ADMIN".equals(u.getRole()))
                .map(u -> StaffListResponse.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .role(u.getRole())
                        .activeOrders(activeOrdersByWaiter.getOrDefault(u.getId(), 0L).intValue())
                        .build())
                .toList();
    }

    private static boolean isCompleted(String status) {
        return status != null && (OrderStatus.COMPLETED.name().equals(status)
                || OrderStatus.SERVED.name().equals(status));
    }

    private static boolean isActive(String status) {
        return status != null && (OrderStatus.PENDING.name().equals(status)
                || OrderStatus.CONFIRMED.name().equals(status)
                || OrderStatus.PREPARING.name().equals(status)
                || OrderStatus.READY.name().equals(status));
    }

    private static <T> List<T> unwrapList(ApiResponse<List<T>> response, String source) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            log.warn("Upstream service '{}' returned invalid response", source);
            return List.of();
        }
        return response.getData();
    }

    private static <T> List<T> unwrapPage(ApiResponse<UserServiceClient.PageDto<T>> response, String source) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            log.warn("Upstream service '{}' returned invalid response", source);
            return List.of();
        }
        var content = response.getData().content();
        return content != null ? content : List.of();
    }

}
