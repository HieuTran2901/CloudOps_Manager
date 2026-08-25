package com.cloudops.manager.operations.controller;

import com.cloudops.manager.common.api.ApiResponse;
import com.cloudops.manager.operations.model.AwsOperationalStatus;
import com.cloudops.manager.operations.model.OperationalEvent;
import com.cloudops.manager.operations.service.OperationsMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/operations", "/api/v1/aws/operations"})
public class OperationsController {

    private final OperationsMonitoringService operationsService;

    public OperationsController(OperationsMonitoringService operationsService) {
        this.operationsService = operationsService;
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AwsOperationalStatus>> getOperationalStatus(
            @RequestParam(required = false) String region) {
        AwsOperationalStatus status = operationsService.getAwsOperationalStatus(region);
        return ResponseEntity.ok(ApiResponse.success(status, "Operational status retrieved successfully."));
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<List<OperationalEvent>>> getOperationalEvents() {
        List<OperationalEvent> events = operationsService.getEventBuffer().getRecentEvents();
        return ResponseEntity.ok(ApiResponse.success(events, "Operational events retrieved successfully."));
    }

    @GetMapping("/freshness")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEvidenceFreshness(
            @RequestParam(required = false) String region) {
        AwsOperationalStatus status = operationsService.getAwsOperationalStatus(region);
        Map<String, Object> freshness = Map.of(
                "status", status.status(),
                "lastSuccessfulSync", status.lastSuccessfulSync() != null ? status.lastSuccessfulSync() : "NONE",
                "lastAttemptedSync", status.lastAttemptedSync(),
                "evidenceAgeSeconds", status.evidenceAgeSeconds() != null ? status.evidenceAgeSeconds() : -1,
                "region", status.region()
        );
        return ResponseEntity.ok(ApiResponse.success(freshness, "Evidence freshness retrieved successfully."));
    }
}