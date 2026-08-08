package az.flowix.report.service;

import az.flowix.common.enums.OrderStatus;
import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.common.type.LocalizedString;
import az.flowix.report.client.MenuServiceClient;
import az.flowix.report.client.OrderServiceClient;
import az.flowix.report.client.UserServiceClient;
import az.flowix.report.client.dto.OrderServiceOrderResponse;
import az.flowix.report.dto.DailyRevenueResponse;
import az.flowix.report.dto.HourlyResponse;
import az.flowix.report.dto.SalesByCategoryResponse;
import az.flowix.report.dto.StaffPerformanceResponse;
import az.flowix.report.dto.SummaryResponse;
import az.flowix.report.dto.TopItemResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final OrderServiceClient orderServiceClient;
    private final MenuServiceClient menuServiceClient;
    private final UserServiceClient userServiceClient;

    public ReportService(OrderServiceClient orderServiceClient, MenuServiceClient menuServiceClient,
                         UserServiceClient userServiceClient) {
        this.orderServiceClient = orderServiceClient;
        this.menuServiceClient = menuServiceClient;
        this.userServiceClient = userServiceClient;
    }

    public SummaryResponse getSummary(UUID orgId) {
        log.info("Fetching report summary for org: {}", orgId);
        var orders = unwrapList(orderServiceClient.getOrders(orgId), "order-service");

        var completed = orders.stream()
                .filter(o -> isCompleted(o.getStatus()))
                .toList();
        var cancelled = orders.stream()
                .filter(o -> isCancelled(o.getStatus()))
                .toList();

        var totalRevenue = completed.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var avgOrderValue = completed.isEmpty() ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(completed.size()), 2, RoundingMode.HALF_UP);

        return SummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .completed(completed.size())
                .cancelled(cancelled.size())
                .avgOrderValue(avgOrderValue)
                .build();
    }

    public List<DailyRevenueResponse> getDailyRevenue(UUID orgId) {
        log.info("Fetching daily revenue for org: {}", orgId);
        var orders = unwrapList(orderServiceClient.getOrders(orgId), "order-service");

        var byDate = orders.stream()
                .filter(o -> isCompleted(o.getStatus()) && o.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        o -> LocalDate.ofInstant(o.getCreatedAt(), ZoneId.systemDefault()).toString(),
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            var revenue = list.stream()
                                    .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            return Map.entry(revenue, list.size());
                        })));

        return byDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .limit(7)
                .map(e -> DailyRevenueResponse.builder()
                        .date(e.getKey())
                        .revenue(e.getValue().getKey())
                        .orderCount(e.getValue().getValue())
                        .build())
                .toList();
    }

    public HourlyResponse getHourly(UUID orgId) {
        log.info("Fetching hourly data for org: {}", orgId);
        var orders = unwrapList(orderServiceClient.getOrders(orgId), "order-service");

        var hourly = new int[24];
        for (var order : orders) {
            if (order.getCreatedAt() != null) {
                var cal = GregorianCalendar.from(order.getCreatedAt().atZone(ZoneId.systemDefault()));
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                hourly[hour]++;
            }
        }

        return new HourlyResponse(hourly);
    }

    public List<SalesByCategoryResponse> getSalesByCategory(UUID orgId) {
        log.info("Fetching sales by category for org: {}", orgId);
        var orders = unwrapList(orderServiceClient.getOrders(orgId), "order-service");
        var items = unwrapList(menuServiceClient.getItems(orgId), "menu-service");
        var categories = unwrapList(menuServiceClient.getCategories(orgId), "menu-service");

        var categoryNames = categories.stream()
                .collect(Collectors.toMap(
                        c -> c.getId(),
                        c -> c.getName() != null && c.getName().getEn() != null ? c.getName().getEn() : ""));

        var itemToCategory = items.stream()
                .filter(i -> i.getCategoryId() != null)
                .collect(Collectors.toMap(i -> i.getId(), i -> i.getCategoryId()));

        var categoryCounts = orders.stream()
                .filter(o -> isCompleted(o.getStatus()))
                .flatMap(o -> o.getItems().stream())
                .filter(i -> i.getMenuItemId() != null)
                .collect(Collectors.groupingBy(
                        i -> itemToCategory.getOrDefault(i.getMenuItemId(), null),
                        Collectors.summingInt(i -> i.getQuantity() != null ? i.getQuantity() : 0)));

        return categoryCounts.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .map(e -> SalesByCategoryResponse.builder()
                        .categoryId(e.getKey())
                        .name(new LocalizedString(null, categoryNames.getOrDefault(e.getKey(), ""), null))
                        .count(e.getValue())
                        .build())
                .toList();
    }

    public List<TopItemResponse> getTopItems(UUID orgId) {
        log.info("Fetching top items for org: {}", orgId);
        var orders = unwrapList(orderServiceClient.getOrders(orgId), "order-service");
        var items = unwrapList(menuServiceClient.getItems(orgId), "menu-service");

        var itemNames = items.stream()
                .collect(Collectors.toMap(
                        i -> i.getId(),
                        i -> i.getName() != null && i.getName().getEn() != null ? i.getName().getEn() : ""));

        var itemData = orders.stream()
                .filter(o -> isCompleted(o.getStatus()))
                .flatMap(o -> o.getItems().stream())
                .filter(i -> i.getMenuItemId() != null)
                .collect(Collectors.groupingBy(
                        i -> i.getMenuItemId(),
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            var qty = list.stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
                            var rev = list.stream()
                                    .map(i -> i.getPrice() != null
                                            ? i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity() != null ? i.getQuantity() : 0))
                                            : BigDecimal.ZERO)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                            return Map.entry(qty, rev);
                        })));

        return itemData.entrySet().stream()
                .sorted((a, b) -> b.getValue().getValue().compareTo(a.getValue().getValue()))
                .limit(8)
                .map(e -> TopItemResponse.builder()
                        .menuItemId(e.getKey())
                        .name(new LocalizedString(null, itemNames.getOrDefault(e.getKey(), ""), null))
                        .count(e.getValue().getKey())
                        .revenue(e.getValue().getValue())
                        .build())
                .toList();
    }

    public List<StaffPerformanceResponse> getStaffPerformance(UUID orgId) {
        log.info("Fetching staff performance for org: {}", orgId);
        var users = unwrapPage(userServiceClient.getUsers(orgId, 100), "access-service");
        var orders = unwrapList(orderServiceClient.getOrders(orgId), "order-service");

        var byWaiter = orders.stream()
                .filter(o -> o.getWaiterId() != null)
                .collect(Collectors.groupingBy(o -> o.getWaiterId()));

        return users.stream()
                .filter(u -> u.getRole() != null && !"ADMIN".equals(u.getRole()))
                .map(u -> {
                    var userOrders = byWaiter.getOrDefault(u.getId(), List.<OrderServiceOrderResponse>of());
                    var total = userOrders.size();
                    var completed = (int) userOrders.stream()
                            .filter(o -> isCompleted(o.getStatus())).count();
                    var revenue = userOrders.stream()
                            .filter(o -> isCompleted(o.getStatus()))
                            .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return StaffPerformanceResponse.builder()
                            .userId(u.getId())
                            .name(u.getName())
                            .role(u.getRole())
                            .totalOrders(total)
                            .completedOrders(completed)
                            .revenue(revenue)
                            .build();
                })
                .toList();
    }

    private static boolean isCompleted(String status) {
        return status != null && (OrderStatus.COMPLETED.name().equals(status)
                || "PAID".equalsIgnoreCase(status));
    }

    private static boolean isCancelled(String status) {
        return status != null && OrderStatus.CANCELLED.name().equals(status);
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
