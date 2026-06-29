package com.vms.controller;

import com.vms.dto.DashboardStatsResponse;
import com.vms.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports & Analytics", description = "Endpoints for dashboard statistics")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/dashboard-stats")
    @Operation(summary = "Get aggregated dashboard statistics")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(reportService.getDashboardStats());
    }
}
