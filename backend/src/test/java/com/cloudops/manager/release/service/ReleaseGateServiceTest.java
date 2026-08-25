package com.cloudops.manager.release.service;

import com.cloudops.manager.aws.preflight.model.DeploymentPreflightResult;
import com.cloudops.manager.aws.preflight.model.PreflightStatus;
import com.cloudops.manager.aws.preflight.service.AwsDeploymentPreflightService;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.operations.service.OperationsMonitoringService;
import com.cloudops.manager.release.model.ReleaseGateResult;
import com.cloudops.manager.release.model.ReleaseGateStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReleaseGateServiceTest {

    private AwsDeploymentPreflightService preflightService;
    private OperationsMonitoringService operationsService;
    private AwsIdentityService identityService;
    private ReleaseGateService releaseGateService;

    @BeforeEach
    void setUp() {
        preflightService = mock(AwsDeploymentPreflightService.class);
        operationsService = mock(OperationsMonitoringService.class);
        identityService = mock(AwsIdentityService.class);

        when(identityService.getCurrentIdentity()).thenReturn(
                new CallerIdentity("351405419700", "arn:aws:iam::351405419700:user/cloud-agent-antigravity", "AIDATEST")
        );

        releaseGateService = new ReleaseGateService(
                preflightService,
                operationsService,
                identityService,
                "1.0.0",
                "release-2026.08-p38",
                "ap-southeast-2"
        );
    }

    @Test
    @DisplayName("Release gate accurately isolates deployment and runtime block from analytics pass (BLK-001)")
    void testReleaseGateWithBlk001() {
        DeploymentPreflightResult preflightBlocked = new DeploymentPreflightResult(
                PreflightStatus.BLOCKED,
                "351405419700",
                "ap-southeast-2",
                "arn:aws:iam::351405419700:user/cloud-agent-antigravity",
                List.of(),
                Instant.now(),
                "ECR DescribeRepositories denied (BLK-001)"
        );
        when(preflightService.runPreflightCheck(any())).thenReturn(preflightBlocked);

        ReleaseGateResult result = releaseGateService.evaluateReleaseGate("ap-southeast-2");
        assertNotNull(result);
        assertTrue(result.analyticsReady());
        assertTrue(result.operationallyReady());
        assertTrue(result.securityReady());
        assertTrue(result.determinismReady());
        assertTrue(result.resilienceReady());
        assertFalse(result.deploymentReady());
        assertFalse(result.runtimeReady());
        assertFalse(result.releaseReady());
        assertEquals(ReleaseGateStatus.BLOCKED, result.overallStatus());
        assertNotNull(result.sha256Digest());
        assertEquals(64, result.sha256Digest().length());
    }

    @Test
    @DisplayName("Release gate passes when all preflight capabilities pass")
    void testReleaseGateAllPass() {
        DeploymentPreflightResult preflightPass = new DeploymentPreflightResult(
                PreflightStatus.PASS,
                "111222333444",
                "us-east-1",
                "arn:aws:iam::111222333444:role/Admin",
                List.of(),
                Instant.now(),
                "All capabilities pass."
        );
        when(preflightService.runPreflightCheck(any())).thenReturn(preflightPass);

        ReleaseGateResult result = releaseGateService.evaluateReleaseGate("us-east-1");
        assertNotNull(result);
        assertTrue(result.deploymentReady());
        assertTrue(result.runtimeReady());
        assertTrue(result.resilienceReady());
        assertTrue(result.releaseReady());
        assertEquals(ReleaseGateStatus.PASS, result.overallStatus());
    }
}