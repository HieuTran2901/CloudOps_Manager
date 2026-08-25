package com.cloudops.manager.aws.drift.controller;

import com.cloudops.manager.aws.drift.model.DriftReport;
import com.cloudops.manager.aws.drift.service.DriftComparisonService;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aws/drift")
public class DriftController {

    private final DriftComparisonService driftService;

    public DriftController(DriftComparisonService driftService) {
        this.driftService = driftService;
    }

    @GetMapping("/supported-resources")
    public ResponseEntity<ApiResponse<List<String>>> getSupportedResourceTypes() {
        List<String> types = driftService.getSupportedResourceTypes();
        return ResponseEntity.ok(ApiResponse.success(types, "Supported Terraform resource types retrieved."));
    }

    @PostMapping("/evaluate")
    public ResponseEntity<ApiResponse<DriftReport>> evaluateDrift(
            @RequestBody String terraformStateJson,
            @RequestParam(required = false) String region) {

        DriftReport report = driftService.evaluateDrift(terraformStateJson, region);
        return ResponseEntity.ok(ApiResponse.success(report, "Drift evaluation completed successfully."));
    }

    @PostMapping("/accounts/{accountId}/evaluate")
    public ResponseEntity<ApiResponse<DriftReport>> evaluateCrossAccountDrift(
            @PathVariable String accountId,
            @RequestParam String roleArn,
            @RequestParam(required = false) String roleSessionName,
            @RequestParam(required = false) String externalId,
            @RequestParam(required = false) String region,
            @RequestBody String terraformStateJson) {

        AwsAccountTarget target = new AwsAccountTarget(accountId, roleArn, roleSessionName, externalId, region);
        DriftReport report = driftService.evaluateCrossAccountDrift(target, terraformStateJson);
        return ResponseEntity.ok(ApiResponse.success(report, "Cross-account drift evaluation completed successfully."));
    }
}