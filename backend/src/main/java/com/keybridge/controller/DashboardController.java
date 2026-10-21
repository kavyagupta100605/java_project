package com.keybridge.controller;

import com.keybridge.dto.dashboard.*;
import com.keybridge.entity.User;
import com.keybridge.service.AuthService;
import com.keybridge.service.DashboardService;
import com.keybridge.service.ExpiryMonitorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthService authService;
    private final ExpiryMonitorService expiryMonitorService;

    public DashboardController(
            DashboardService dashboardService,
            AuthService authService,
            ExpiryMonitorService expiryMonitorService
    ) {
        this.dashboardService = dashboardService;
        this.authService = authService;
        this.expiryMonitorService = expiryMonitorService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        User currentUser = authService.getCurrentUserEntity();
        return ResponseEntity.ok(dashboardService.getStats(currentUser));
    }

    @GetMapping("/dashboard/activity")
    public ResponseEntity<List<ActivityPoint>> getActivity(@RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(dashboardService.getActivity(days));
    }

    @GetMapping("/dashboard/expiry-timeline")
    public ResponseEntity<List<ExpiryPoint>> getExpiryTimeline(@RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(dashboardService.getExpiryTimeline(months));
    }

    @GetMapping("/dashboard/status-distribution")
    public ResponseEntity<Map<String, Long>> getStatusDistribution() {
        return ResponseEntity.ok(dashboardService.getStatusDistribution());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam(defaultValue = "") String q) {
        User currentUser = authService.getCurrentUserEntity();
        return ResponseEntity.ok(dashboardService.search(q, currentUser));
    }

    @PostMapping("/certs/refresh-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> refreshCertStatuses() {
        int updated = expiryMonitorService.refreshStatuses();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "updatedCount", updated,
                "message", "Certificate statuses refreshed successfully"
        ));
    }
}
