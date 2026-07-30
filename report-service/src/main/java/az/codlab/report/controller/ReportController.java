package az.codlab.report.controller;

import az.codlab.common.exception.handling.dto.ApiResponse;
import az.codlab.report.dto.DailyRevenueResponse;
import az.codlab.report.dto.HourlyResponse;
import az.codlab.report.dto.SalesByCategoryResponse;
import az.codlab.report.dto.StaffPerformanceResponse;
import az.codlab.report.dto.SummaryResponse;
import az.codlab.report.dto.TopItemResponse;
import az.codlab.report.service.ReportService;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<SummaryResponse>> getSummary(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getSummary(orgId)));
    }

    @GetMapping("/daily-revenue")
    public ResponseEntity<ApiResponse<List<DailyRevenueResponse>>> getDailyRevenue(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getDailyRevenue(orgId)));
    }

    @GetMapping("/hourly")
    public ResponseEntity<ApiResponse<HourlyResponse>> getHourly(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getHourly(orgId)));
    }

    @GetMapping("/sales-by-category")
    public ResponseEntity<ApiResponse<List<SalesByCategoryResponse>>> getSalesByCategory(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getSalesByCategory(orgId)));
    }

    @GetMapping("/top-items")
    public ResponseEntity<ApiResponse<List<TopItemResponse>>> getTopItems(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getTopItems(orgId)));
    }

    @GetMapping("/staff-performance")
    public ResponseEntity<ApiResponse<List<StaffPerformanceResponse>>> getStaffPerformance(@RequestParam UUID orgId) {
        return ResponseEntity.ok(ApiResponse.success(reportService.getStaffPerformance(orgId)));
    }

}
