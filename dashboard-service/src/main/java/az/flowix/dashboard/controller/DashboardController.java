package az.flowix.dashboard.controller;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.dashboard.dto.DashboardStatsResponse;
import az.flowix.dashboard.dto.RecentOrderResponse;
import az.flowix.dashboard.dto.StaffListResponse;
import az.flowix.dashboard.dto.TopItemResponse;
import az.flowix.dashboard.service.DashboardService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("@perm.has('dashboard.view')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStats(orgId)));
    }

    @GetMapping("/top-items")
    @PreAuthorize("@perm.has('dashboard.view')")
    public ResponseEntity<ApiResponse<List<TopItemResponse>>> getTopItems(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getTopItems(orgId)));
    }

    @GetMapping("/recent-orders")
    @PreAuthorize("@perm.has('dashboard.view')")
    public ResponseEntity<ApiResponse<List<RecentOrderResponse>>> getRecentOrders(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getRecentOrders(orgId)));
    }

    @GetMapping("/staff-list")
    @PreAuthorize("@perm.has('dashboard.view')")
    public ResponseEntity<ApiResponse<List<StaffListResponse>>> getStaffList(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getStaffList(orgId)));
    }

}
