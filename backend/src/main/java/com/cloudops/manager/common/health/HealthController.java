package com.cloudops.manager.common.health;

import com.cloudops.manager.common.api.ApiResponse;
import com.cloudops.manager.operations.model.DetailedHealthResponse;
import com.cloudops.manager.operations.service.OperationsMonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/health", "/api/v1/aws/health"})
public class HealthController {

    private final OperationsMonitoringService operationsService;

    public HealthController(OperationsMonitoringService operationsService) {
        this.operationsService = operationsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DetailedHealthResponse>> checkHealth() {
        DetailedHealthResponse detailed = operationsService.getDetailedHealth();
        return ResponseEntity.ok(ApiResponse.success(detailed, "Service is healthy."));
    }

    @GetMapping("/live")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkLiveness() {
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("status", "UP"),
                "Liveness probe succeeded."
        ));
    }

    @GetMapping("/ready")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkReadiness() {
        DetailedHealthResponse health = operationsService.getDetailedHealth();
        return ResponseEntity.ok(ApiResponse.success(
                Map.of(
                        "status", health.status(),
                        "ready", "UP".equalsIgnoreCase(health.status()),
                        "service", health.service(),
                        "version", health.version(),
                        "release", health.release()
                ),
                "Readiness probe succeeded."
        ));
    }
}