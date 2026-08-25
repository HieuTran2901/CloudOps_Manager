package com.cloudops.manager.operations.resilience.service;

import com.cloudops.manager.aws.preflight.model.DeploymentPreflightResult;
import com.cloudops.manager.aws.preflight.model.PreflightStatus;
import com.cloudops.manager.aws.preflight.service.AwsDeploymentPreflightService;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.operations.evidence.service.EvidenceLifecycleService;
import com.cloudops.manager.operations.incident.service.IncidentManagementService;
import com.cloudops.manager.operations.resilience.model.OperationalResilienceEvaluation;
import com.cloudops.manager.operations.resilience.model.VerificationScenarioResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationalResilienceServiceTest {

    private IncidentManagementService incidentService;
    private EvidenceLifecycleService evidenceService;
    private AwsDeploymentPreflightService preflightService;
    private AwsIdentityService identityService;
    private OperationalResilienceService resilienceService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentManagementService();
        evidenceService = new EvidenceLifecycleService("ap-southeast-2");
        preflightService = mock(AwsDeploymentPreflightService.class);
        identityService = mock(AwsIdentityService.class);

        when(identityService.getCurrentIdentity()).thenReturn(
                new CallerIdentity("351405419700", "arn:aws:iam::351405419700:user/cloud-agent-antigravity", "AIDATEST")
        );

        when(preflightService.runPreflightCheck(any())).thenReturn(
                new DeploymentPreflightResult(PreflightStatus.BLOCKED, "351405419700", "ap-southeast-2", "arn:...", List.of(), Instant.now(), "BLK-001")
        );

        resilienceService = new OperationalResilienceService(
                incidentService,
                evidenceService,
                preflightService,
                identityService,
                "ap-southeast-2"
        );
    }

    @Test
    @DisplayName("Evaluates operational resilience with deterministic digest and deployment boundary tracking")
    void testEvaluateResilience() {
        OperationalResilienceEvaluation eval = resilienceService.evaluateResilience("ap-southeast-2");
        assertNotNull(eval);
        assertTrue(eval.isResilient());
        assertEquals("351405419700", eval.accountId());
        assertEquals("ap-southeast-2", eval.region());
        assertEquals("BLOCKED", eval.dimensionStates().get("deploymentState"));
        assertEquals("PASS", eval.dimensionStates().get("discoveryHealth"));
        assertNotNull(eval.canonicalDigest());
        assertEquals(64, eval.canonicalDigest().length());
    }

    @Test
    @DisplayName("Runs simulated analytical verification scenarios")
    void testVerificationScenarios() {
        List<VerificationScenarioResult> scenarios = resilienceService.runSimulatedVerificationScenarios();
        assertNotNull(scenarios);
        assertFalse(scenarios.isEmpty());
        for (VerificationScenarioResult s : scenarios) {
            assertTrue(s.isSimulated());
            assertEquals("PASS", s.status());
        }
    }
}