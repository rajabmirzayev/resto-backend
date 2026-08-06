package az.flowix.report.controller;

import az.flowix.common.exception.handling.dto.ApiResponse;
import az.flowix.report.dto.DailyRevenueResponse;
import az.flowix.report.dto.HourlyResponse;
import az.flowix.report.dto.SalesByCategoryResponse;
import az.flowix.report.dto.StaffPerformanceResponse;
import az.flowix.report.dto.SummaryResponse;
import az.flowix.report.dto.TopItemResponse;
import az.flowix.report.service.ReportService;

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
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) { this.reportService = reportService; }

    @GetMapping("/summary")
    @PreAuthorize("@perm.has('report.view')")
    public ResponseEntity<ApiResponse<SummaryResponse>> getSummary(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getSummary(orgId)));
    }

    @GetMapping("/daily-revenue")
    @PreAuthorize("@perm.has('report.view')")
    public ResponseEntity<ApiResponse<List<DailyRevenueResponse>>> getDailyRevenue(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getDailyRevenue(orgId)));
    }

    @GetMapping("/hourly")
    @PreAuthorize("@perm.has('report.view')")
    public ResponseEntity<ApiResponse<HourlyResponse>> getHourly(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getHourly(orgId)));
    }

    @GetMapping("/sales-by-category")
    @PreAuthorize("@perm.has('report.view')")
    public ResponseEntity<ApiResponse<List<SalesByCategoryResponse>>> getSalesByCategory(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getSalesByCategory(orgId)));
    }

    @GetMapping("/top-items")
    @PreAuthorize("@perm.has('report.view')")
    public ResponseEntity<ApiResponse<List<TopItemResponse>>> getTopItems(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getTopItems(orgId)));
    }

    @GetMapping("/staff-performance")
    @PreAuthorize("@perm.has('report.view')")
    public ResponseEntity<ApiResponse<List<StaffPerformanceResponse>>> getStaffPerformance(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getStaffPerformance(orgId)));
    }

}
