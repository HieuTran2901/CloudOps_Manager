package com.cloudops.manager.aws.preflight.controller;

import com.cloudops.manager.aws.preflight.model.DeploymentPreflightResult;
import com.cloudops.manager.aws.preflight.service.AwsDeploymentPreflightService;
import com.cloudops.manager.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/aws/preflight", "/api/v1/preflight"})
public class AwsDeploymentPreflightController {

    private final AwsDeploymentPreflightService preflightService;

    public AwsDeploymentPreflightController(AwsDeploymentPreflightService preflightService) {
        this.preflightService = preflightService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<DeploymentPreflightResult>> runPreflightCheck(
            @RequestParam(required = false) String region) {
        DeploymentPreflightResult result = preflightService.runPreflightCheck(region);
        return ResponseEntity.ok(ApiResponse.success(result, "Deployment preflight evaluation completed."));
    }
}