package az.codlab.dashboard.controller;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.dashboard.dto.DashboardStatsResponse;
import az.codlab.dashboard.dto.RecentOrderResponse;
import az.codlab.dashboard.dto.StaffListResponse;
import az.codlab.dashboard.dto.TopItemResponse;
import az.codlab.dashboard.service.DashboardService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) { this.dashboardService = dashboardService; }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats(orgId)));
    }

    @GetMapping("/top-items")
    public ResponseEntity<ApiResponse<List<TopItemResponse>>> getTopItems(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getTopItems(orgId)));
    }

    @GetMapping("/recent-orders")
    public ResponseEntity<ApiResponse<List<RecentOrderResponse>>> getRecentOrders(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getRecentOrders(orgId)));
    }

    @GetMapping("/staff-list")
    public ResponseEntity<ApiResponse<List<StaffListResponse>>> getStaffList(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStaffList(orgId)));
    }

}
