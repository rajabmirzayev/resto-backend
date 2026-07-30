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
        return new DashboardStatsResponse(null, 0, 0, 0);
    }

    public List<TopItemResponse> getTopItems(UUID orgId) {
        log.debug("Fetching top items for org: {}", orgId);
        return List.of();
    }

    public List<RecentOrderResponse> getRecentOrders(UUID orgId) {
        log.debug("Fetching recent orders for org: {}", orgId);
        return List.of();
    }

    public List<StaffListResponse> getStaffList(UUID orgId) {
        log.debug("Fetching staff list for org: {}", orgId);
        return List.of();
    }

}
