package com.cloudops.manager.aws.compliance.controller;

import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationReport;
import com.cloudops.manager.aws.compliance.model.ComplianceRuleDefinition;
import com.cloudops.manager.aws.compliance.service.ComplianceEvaluationService;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aws/compliance")
public class ComplianceController {

    private final ComplianceEvaluationService complianceService;

    public ComplianceController(ComplianceEvaluationService complianceService) {
        this.complianceService = complianceService;
    }

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<ComplianceRuleDefinition>>> getRegisteredRules() {
        List<ComplianceRuleDefinition> rules = complianceService.getRegisteredRules().stream()
                .map(ComplianceRuleDefinition::fromRule)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(rules, "Registered compliance rules retrieved successfully."));
    }

    @GetMapping("/evaluate")
    public ResponseEntity<ApiResponse<ComplianceEvaluationReport>> evaluate(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) List<String> rules) {

        ComplianceEvaluationReport report = complianceService.evaluateLocal(region, rules);
        return ResponseEntity.ok(ApiResponse.success(report, "Compliance evaluation completed successfully."));
    }

    @GetMapping("/accounts/{accountId}/evaluate")
    public ResponseEntity<ApiResponse<ComplianceEvaluationReport>> evaluateCrossAccount(
            @PathVariable String accountId,
            @RequestParam String roleArn,
            @RequestParam(required = false) String roleSessionName,
            @RequestParam(required = false) String externalId,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) List<String> rules) {

        AwsAccountTarget target = new AwsAccountTarget(accountId, roleArn, roleSessionName, externalId, region);
        ComplianceEvaluationReport report = complianceService.evaluateCrossAccount(target, rules);
        return ResponseEntity.ok(ApiResponse.success(report, "Cross-account compliance evaluation completed successfully."));
    }
}