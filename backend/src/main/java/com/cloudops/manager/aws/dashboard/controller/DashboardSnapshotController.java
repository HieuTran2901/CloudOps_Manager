package com.cloudops.manager.aws.dashboard.controller;

import com.cloudops.manager.aws.dashboard.model.DashboardSnapshot;
import com.cloudops.manager.aws.dashboard.service.DashboardSnapshotService;
import com.cloudops.manager.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/aws/dashboard")
public class DashboardSnapshotController {

    private static final Logger log = LoggerFactory.getLogger(DashboardSnapshotController.class);
    private final DashboardSnapshotService snapshotService;

    public DashboardSnapshotController(DashboardSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @GetMapping("/snapshot")
    public ResponseEntity<ApiResponse<DashboardSnapshot>> getDashboardSnapshot(
            @RequestParam(name = "region", required = false) String region) {
        log.info("Received request for DashboardSnapshot in region: {}", region);
        DashboardSnapshot snapshot = snapshotService.getSnapshot(region);
        return ResponseEntity.ok(ApiResponse.success(snapshot, "Dashboard snapshot retrieved successfully."));
    }

    @PostMapping("/snapshot/refresh")
    public ResponseEntity<ApiResponse<DashboardSnapshot>> refreshDashboardSnapshot(
            @RequestParam(name = "region", required = false) String region) {
        log.info("Received forced refresh request for DashboardSnapshot in region: {}", region);
        DashboardSnapshot snapshot = snapshotService.refreshSnapshot(region);
        return ResponseEntity.ok(ApiResponse.success(snapshot, "Dashboard snapshot refreshed successfully."));
    }
}
