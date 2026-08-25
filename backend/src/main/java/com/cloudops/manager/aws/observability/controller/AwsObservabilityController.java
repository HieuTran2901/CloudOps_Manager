package com.cloudops.manager.aws.observability.controller;

import com.cloudops.manager.aws.observability.model.TelemetryAggregationResult;
import com.cloudops.manager.aws.observability.service.AwsObservabilityService;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.common.api.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/aws/observability")
public class AwsObservabilityController {

    private final AwsObservabilityService observabilityService;

    public AwsObservabilityController(AwsObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<TelemetryAggregationResult>> getMetrics(
            @RequestParam String resourceType,
            @RequestParam List<String> resourceIds,
            @RequestParam List<String> metricNames,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(required = false, defaultValue = "300") Integer period,
            @RequestParam(required = false, defaultValue = "Average") String statistic,
            @RequestParam(required = false) Integer downsampleFactor,
            @RequestParam(required = false) String rollupStatistic,
            @RequestParam(required = false) String region) {

        TelemetryAggregationResult result = observabilityService.getAggregatedMetrics(
                resourceType, resourceIds, metricNames, startTime, endTime, period, statistic,
                downsampleFactor, rollupStatistic, region
        );
        return ResponseEntity.ok(ApiResponse.success(result, "Telemetry metrics retrieved successfully."));
    }

    @GetMapping("/accounts/{accountId}/metrics")
    public ResponseEntity<ApiResponse<TelemetryAggregationResult>> getCrossAccountMetrics(
            @PathVariable String accountId,
            @RequestParam String roleArn,
            @RequestParam(required = false) String roleSessionName,
            @RequestParam(required = false) String externalId,
            @RequestParam String resourceType,
            @RequestParam List<String> resourceIds,
            @RequestParam List<String> metricNames,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(required = false, defaultValue = "300") Integer period,
            @RequestParam(required = false, defaultValue = "Average") String statistic,
            @RequestParam(required = false) Integer downsampleFactor,
            @RequestParam(required = false) String rollupStatistic,
            @RequestParam(required = false) String region) {

        AwsAccountTarget target = new AwsAccountTarget(accountId, roleArn, roleSessionName, externalId, region);
        TelemetryAggregationResult result = observabilityService.getCrossAccountMetrics(
                target, resourceType, resourceIds, metricNames, startTime, endTime, period, statistic,
                downsampleFactor, rollupStatistic
        );
        return ResponseEntity.ok(ApiResponse.success(result, "Cross-account telemetry metrics retrieved successfully."));
    }
}