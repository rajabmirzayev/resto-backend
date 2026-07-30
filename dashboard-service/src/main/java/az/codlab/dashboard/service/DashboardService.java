package az.codlab.dashboard.service;

import az.codlab.dashboard.dto.DashboardStatsResponse;
import az.codlab.dashboard.dto.RecentOrderResponse;
import az.codlab.dashboard.dto.StaffListResponse;
import az.codlab.dashboard.dto.TopItemResponse;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    // TODO: order-service, table-service ve menu-service-e HTTP call-lar

    public DashboardStatsResponse getStats(UUID orgId) {
        log.debug("Fetching dashboard stats for org: {}", orgId);
        // TODO: order-service-den (completedOrders, activeOrders) + table-service-den (occupiedTables) + totalRevenue (completed order-lardan)
        return new DashboardStatsResponse(null, 0, 0, 0);
    }

    public List<TopItemResponse> getTopItems(UUID orgId) {
        log.debug("Fetching top items for org: {}", orgId);
        // TODO: order-service + menu-service birlesdirerek en cox satilan 5 mehsulu qaytar
        return List.of();
    }

    public List<RecentOrderResponse> getRecentOrders(UUID orgId) {
        log.debug("Fetching recent orders for org: {}", orgId);
        // TODO: order-service-den son 6 sifarisi createdAt DESC gotur
        return List.of();
    }

    public List<StaffListResponse> getStaffList(UUID orgId) {
        log.debug("Fetching staff list for org: {}", orgId);
        // TODO: user-service-den (staff) + order-service-den (activeOrders count)
        return List.of();
    }

}
