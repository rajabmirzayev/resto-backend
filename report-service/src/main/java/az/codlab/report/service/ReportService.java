package az.codlab.report.service;

import az.codlab.report.dto.DailyRevenueResponse;
import az.codlab.report.dto.HourlyResponse;
import az.codlab.report.dto.SalesByCategoryResponse;
import az.codlab.report.dto.StaffPerformanceResponse;
import az.codlab.report.dto.SummaryResponse;
import az.codlab.report.dto.TopItemResponse;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    // TODO: order-service ve menu-service-e HTTP call-lar

    public SummaryResponse getSummary(UUID orgId) {
        log.debug("Fetching report summary for org: {}", orgId);
        return new SummaryResponse(null, 0, 0, null);
    }

    public List<DailyRevenueResponse> getDailyRevenue(UUID orgId) {
        log.debug("Fetching daily revenue for org: {}", orgId);
        return List.of();
    }

    public HourlyResponse getHourly(UUID orgId) {
        log.debug("Fetching hourly data for org: {}", orgId);
        return new HourlyResponse(new int[24]);
    }

    public List<SalesByCategoryResponse> getSalesByCategory(UUID orgId) {
        log.debug("Fetching sales by category for org: {}", orgId);
        return List.of();
    }

    public List<TopItemResponse> getTopItems(UUID orgId) {
        log.debug("Fetching top items for org: {}", orgId);
        return List.of();
    }

    public List<StaffPerformanceResponse> getStaffPerformance(UUID orgId) {
        log.debug("Fetching staff performance for org: {}", orgId);
        return List.of();
    }

}
