package com.cloudops.manager.operations.controller;

import com.cloudops.manager.common.api.ApiResponse;
import com.cloudops.manager.operations.evidence.model.EvidenceLifecycleRecord;
import com.cloudops.manager.operations.evidence.service.EvidenceLifecycleService;
import com.cloudops.manager.operations.incident.model.IncidentRecord;
import com.cloudops.manager.operations.incident.service.IncidentManagementService;
import com.cloudops.manager.operations.resilience.model.OperationalResilienceEvaluation;
import com.cloudops.manager.operations.resilience.model.VerificationScenarioResult;
import com.cloudops.manager.operations.resilience.service.OperationalResilienceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/operations", "/api/v1/resilience"})
public class OperationalResilienceController {

    private final IncidentManagementService incidentService;
    private final EvidenceLifecycleService evidenceService;
    private final OperationalResilienceService resilienceService;

    public OperationalResilienceController(
            IncidentManagementService incidentService,
            EvidenceLifecycleService evidenceService,
            OperationalResilienceService resilienceService) {
        this.incidentService = incidentService;
        this.evidenceService = evidenceService;
        this.resilienceService = resilienceService;
    }

    @GetMapping("/incidents")
    public ResponseEntity<ApiResponse<List<IncidentRecord>>> getAllIncidents() {
        return ResponseEntity.ok(ApiResponse.success(incidentService.getAllIncidents(), "Incidents retrieved successfully."));
    }

    @GetMapping("/incidents/active")
    public ResponseEntity<ApiResponse<List<IncidentRecord>>> getActiveIncidents() {
        return ResponseEntity.ok(ApiResponse.success(incidentService.getActiveIncidents(), "Active incidents retrieved successfully."));
    }

    @GetMapping("/resilience")
    public ResponseEntity<ApiResponse<OperationalResilienceEvaluation>> getResilienceEvaluation(
            @RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(resilienceService.evaluateResilience(region), "Operational resilience evaluated."));
    }

    @GetMapping("/evidence")
    public ResponseEntity<ApiResponse<List<EvidenceLifecycleRecord>>> getEvidenceLifecycles(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(evidenceService.getEvidenceLifecycles(accountId, region), "Evidence lifecycles retrieved."));
    }

    @GetMapping("/resilience/verification")
    public ResponseEntity<ApiResponse<List<VerificationScenarioResult>>> getVerificationScenarios() {
        return ResponseEntity.ok(ApiResponse.success(resilienceService.runSimulatedVerificationScenarios(), "Verification scenarios executed."));
    }
}