package com.cloudops.manager.aws.sts.controller;

import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/aws")
public class AwsIdentityController {

    private final AwsIdentityService awsIdentityService;

    public AwsIdentityController(AwsIdentityService awsIdentityService) {
        this.awsIdentityService = awsIdentityService;
    }

    @GetMapping("/identity")
    public ResponseEntity<ApiResponse<CallerIdentity>> getCallerIdentity() {
        CallerIdentity identity = awsIdentityService.getCurrentIdentity();
        return ResponseEntity.ok(ApiResponse.success(identity, "Caller identity resolved successfully."));
    }
}