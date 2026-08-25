package com.cloudops.manager.aws.preflight.service;

import com.cloudops.manager.aws.preflight.model.DeploymentPreflightResult;
import com.cloudops.manager.aws.preflight.model.PreflightStatus;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.common.exception.AwsAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AwsDeploymentPreflightServiceTest {

    private AwsIdentityService identityService;
    private AwsDeploymentPreflightService preflightService;

    @BeforeEach
    void setUp() {
        identityService = mock(AwsIdentityService.class);
        preflightService = new AwsDeploymentPreflightService(identityService, "us-east-1");
    }

    @Test
    @DisplayName("Preflight check with healthy IAM identity passes with PASS overall status")
    void testPreflightCheckHealthy() {
        when(identityService.getCurrentIdentity()).thenReturn(
                new CallerIdentity("111222333444", "arn:aws:iam::111222333444:role/AdminRole", "AIDAROOT")
        );

        DeploymentPreflightResult result = preflightService.runPreflightCheck("us-east-1");
        assertNotNull(result);
        assertEquals(PreflightStatus.PASS, result.overallStatus());
        assertEquals("111222333444", result.accountId());
        assertFalse(result.capabilityChecks().isEmpty());
    }

    @Test
    @DisplayName("Preflight check for BLK-001 user accurately identifies ECR limitation and marks status BLOCKED")
    void testPreflightCheckBlk001User() {
        when(identityService.getCurrentIdentity()).thenReturn(
                new CallerIdentity("351405419700", "arn:aws:iam::351405419700:user/cloud-agent-antigravity", "AIDAUSER")
        );

        DeploymentPreflightResult result = preflightService.runPreflightCheck("ap-southeast-2");
        assertNotNull(result);
        assertEquals(PreflightStatus.BLOCKED, result.overallStatus());
        assertEquals("351405419700", result.accountId());
        assertTrue(result.summary().contains("BLK-001"));
    }

    @Test
    @DisplayName("Preflight check with unauthenticated STS returns ACCESS_DENIED")
    void testPreflightCheckStsFailure() {
        when(identityService.getCurrentIdentity()).thenThrow(
                new AwsAccessDeniedException("User not authorized")
        );

        DeploymentPreflightResult result = preflightService.runPreflightCheck("us-east-1");
        assertNotNull(result);
        assertEquals(PreflightStatus.ACCESS_DENIED, result.overallStatus());
    }
}