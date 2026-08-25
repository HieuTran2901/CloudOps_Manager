package com.cloudops.manager.aws.audit.controller;

import com.cloudops.manager.aws.audit.model.CloudTrailEventResult;
import com.cloudops.manager.aws.audit.service.CloudTrailAuditService;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.common.api.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/aws/audit")
public class CloudTrailAuditController {

    private final CloudTrailAuditService auditService;

    public CloudTrailAuditController(CloudTrailAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/cloudtrail/events")
    public ResponseEntity<ApiResponse<CloudTrailEventResult>> lookupEvents(
            @RequestParam(required = false) String eventName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String resourceName,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(required = false, defaultValue = "50") Integer maxResults,
            @RequestParam(required = false) String region) {

        CloudTrailEventResult result = auditService.lookupEvents(
                eventName, username, resourceName, resourceType, startTime, endTime, maxResults, region
        );
        return ResponseEntity.ok(ApiResponse.success(result, "CloudTrail audit events retrieved successfully."));
    }

    @GetMapping("/accounts/{accountId}/cloudtrail/events")
    public ResponseEntity<ApiResponse<CloudTrailEventResult>> lookupCrossAccountEvents(
            @PathVariable String accountId,
            @RequestParam String roleArn,
            @RequestParam(required = false) String roleSessionName,
            @RequestParam(required = false) String externalId,
            @RequestParam(required = false) String eventName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String resourceName,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(required = false, defaultValue = "50") Integer maxResults,
            @RequestParam(required = false) String region) {

        AwsAccountTarget target = new AwsAccountTarget(accountId, roleArn, roleSessionName, externalId, region);
        CloudTrailEventResult result = auditService.lookupCrossAccountEvents(
                target, eventName, username, resourceName, resourceType, startTime, endTime, maxResults
        );
        return ResponseEntity.ok(ApiResponse.success(result, "Cross-account CloudTrail audit events retrieved successfully."));
    }
}