package com.cloudops.manager.aws.federation.controller;

import com.cloudops.manager.aws.federation.model.AwsAccountContext;
import com.cloudops.manager.aws.federation.model.FederationRequest;
import com.cloudops.manager.aws.federation.model.FederationResult;
import com.cloudops.manager.aws.federation.service.AwsFederationService;
import com.cloudops.manager.common.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/aws/federation", "/api/v1/federation"})
public class AwsFederationController {

    private final AwsFederationService federationService;

    public AwsFederationController(AwsFederationService federationService) {
        this.federationService = federationService;
    }

    @PostMapping("/assume-role")
    public ResponseEntity<ApiResponse<FederationResult>> assumeRoleFederation(
            @RequestBody FederationRequest request) {
        FederationResult result = federationService.federateAccount(request);
        return ResponseEntity.ok(ApiResponse.success(result, "Federation evaluated successfully."));
    }

    @GetMapping("/current-context")
    public ResponseEntity<ApiResponse<AwsAccountContext>> getCurrentAccountContext() {
        AwsAccountContext ctx = federationService.getCurrentContext();
        return ResponseEntity.ok(ApiResponse.success(ctx, "Current account context retrieved successfully."));
    }

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<AwsAccountContext>>> listConfiguredAccounts() {
        List<AwsAccountContext> accounts = federationService.listConfiguredAccounts();
        return ResponseEntity.ok(ApiResponse.success(accounts, "Configured account list retrieved successfully."));
    }
}