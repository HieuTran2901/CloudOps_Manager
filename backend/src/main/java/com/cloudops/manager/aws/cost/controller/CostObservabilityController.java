package com.cloudops.manager.aws.cost.controller;

import com.cloudops.manager.aws.cost.model.CostAggregationResult;
import com.cloudops.manager.aws.cost.service.CostObservabilityService;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.common.api.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/aws/costs")
public class CostObservabilityController {

    private final CostObservabilityService costObservabilityService;

    public CostObservabilityController(CostObservabilityService costObservabilityService) {
        this.costObservabilityService = costObservabilityService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CostAggregationResult>> getCostAndUsage(
            @RequestParam(required = false, defaultValue = "UnblendedCost") String metric,
            @RequestParam(required = false, defaultValue = "MONTHLY") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<String> groupBy) {

        CostAggregationResult result = costObservabilityService.getCostAndUsage(
                metric, granularity, startDate, endDate, groupBy, null
        );
        return ResponseEntity.ok(ApiResponse.success(result, "Cost and usage data retrieved successfully."));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<ApiResponse<CostAggregationResult>> getCrossAccountCostAndUsage(
            @PathVariable String accountId,
            @RequestParam String roleArn,
            @RequestParam(required = false) String roleSessionName,
            @RequestParam(required = false) String externalId,
            @RequestParam(required = false, defaultValue = "UnblendedCost") String metric,
            @RequestParam(required = false, defaultValue = "MONTHLY") String granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<String> groupBy) {

        AwsAccountTarget target = new AwsAccountTarget(accountId, roleArn, roleSessionName, externalId, "us-east-1");
        CostAggregationResult result = costObservabilityService.getCrossAccountCostAndUsage(
                target, metric, granularity, startDate, endDate, groupBy, null
        );
        return ResponseEntity.ok(ApiResponse.success(result, "Cross-account cost and usage data retrieved successfully."));
    }
}