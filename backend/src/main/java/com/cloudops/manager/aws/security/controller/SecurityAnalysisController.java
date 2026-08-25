package com.cloudops.manager.aws.security.controller;

import com.cloudops.manager.aws.security.model.*;
import com.cloudops.manager.aws.security.service.SecurityAnalysisService;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aws/security")
public class SecurityAnalysisController {

    private final SecurityAnalysisService securityService;

    public SecurityAnalysisController(SecurityAnalysisService securityService) {
        this.securityService = securityService;
    }

    @GetMapping("/blast-radius/{nodeId}")
    public ResponseEntity<ApiResponse<BlastRadiusResult>> getBlastRadius(
            @PathVariable String nodeId,
            @RequestParam(defaultValue = "3") int maxDepth,
            @RequestParam(required = false) String region) {

        BlastRadiusResult result = securityService.getBlastRadius(nodeId, maxDepth, region);
        return ResponseEntity.ok(ApiResponse.success(result, "Blast radius calculated successfully."));
    }

    @GetMapping("/reachability")
    public ResponseEntity<ApiResponse<SecurityReachabilityResult>> getReachability(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(defaultValue = "5") int maxDepth,
            @RequestParam(required = false) String region) {

        SecurityReachabilityResult result = securityService.getReachability(from, to, maxDepth, region);
        return ResponseEntity.ok(ApiResponse.success(result, "Reachability analyzed successfully."));
    }

    @GetMapping("/exposures")
    public ResponseEntity<ApiResponse<List<SecurityExposureResult>>> getExposures(
            @RequestParam(required = false) String region) {

        List<SecurityExposureResult> results = securityService.getExposures(region);
        return ResponseEntity.ok(ApiResponse.success(results, "Security exposures evaluated successfully."));
    }

    @GetMapping("/lateral-movement")
    public ResponseEntity<ApiResponse<List<LateralMovementResult>>> getLateralMovement(
            @RequestParam(defaultValue = "3") int maxDepth,
            @RequestParam(required = false) String region) {

        List<LateralMovementResult> results = securityService.getLateralMovement(maxDepth, region);
        return ResponseEntity.ok(ApiResponse.success(results, "Lateral movement propagation analyzed successfully."));
    }

    @GetMapping("/accounts/{accountId}/blast-radius/{nodeId}")
    public ResponseEntity<ApiResponse<BlastRadiusResult>> getCrossAccountBlastRadius(
            @PathVariable String accountId,
            @PathVariable String nodeId,
            @RequestParam String roleArn,
            @RequestParam(required = false) String roleSessionName,
            @RequestParam(required = false) String externalId,
            @RequestParam(defaultValue = "3") int maxDepth,
            @RequestParam(required = false) String region) {

        AwsAccountTarget target = new AwsAccountTarget(accountId, roleArn, roleSessionName, externalId, region);
        BlastRadiusResult result = securityService.getCrossAccountBlastRadius(target, nodeId, maxDepth);
        return ResponseEntity.ok(ApiResponse.success(result, "Cross-account blast radius calculated successfully."));
    }
}