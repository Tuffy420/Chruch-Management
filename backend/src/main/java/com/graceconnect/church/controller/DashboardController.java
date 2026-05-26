package com.graceconnect.church.controller;

import com.graceconnect.church.dto.ApiResponse;
import com.graceconnect.church.dto.DashboardSummary;
import com.graceconnect.church.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard Module", description = "Endpoints for fetching unified church statistics, historical charts, and audit telemetry.")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Get Dashboard Summary Metrics", description = "Retrieves current counts of families, members, scheduled functions, total incomes/expenses, dynamic monthly charts, and recent activity logs.")
    public ResponseEntity<ApiResponse<DashboardSummary>> getSummary() {
        DashboardSummary summary = dashboardService.getSummary();
        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved successfully", summary));
    }
}
