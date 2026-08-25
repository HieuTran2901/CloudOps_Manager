package com.cloudops.manager.release.controller;

import com.cloudops.manager.common.api.ApiResponse;
import com.cloudops.manager.release.model.ReleaseGateResult;
import com.cloudops.manager.release.service.ReleaseGateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/release/gate", "/api/v1/release-gate"})
public class ReleaseGateController {

    private final ReleaseGateService releaseGateService;

    public ReleaseGateController(ReleaseGateService releaseGateService) {
        this.releaseGateService = releaseGateService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ReleaseGateResult>> getReleaseGate(
            @RequestParam(required = false) String region) {
        ReleaseGateResult result = releaseGateService.evaluateReleaseGate(region);
        return ResponseEntity.ok(ApiResponse.success(result, "Release gate evaluated successfully."));
    }
}