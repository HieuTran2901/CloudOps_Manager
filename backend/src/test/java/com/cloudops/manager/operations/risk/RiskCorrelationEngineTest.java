package com.cloudops.manager.operations.risk;

import com.cloudops.manager.aws.compliance.model.ComplianceCategory;
import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationResult;
import com.cloudops.manager.aws.compliance.model.ComplianceStatus;
import com.cloudops.manager.aws.drift.model.DriftAttributeDifference;
import com.cloudops.manager.aws.drift.model.DriftResourceResult;
import com.cloudops.manager.aws.drift.model.DriftStatus;
import com.cloudops.manager.aws.quota.model.QuotaStatus;
import com.cloudops.manager.aws.quota.model.ServiceQuotaItem;
import com.cloudops.manager.aws.security.model.ExposureStatus;
import com.cloudops.manager.aws.security.model.SecurityExposureResult;
import com.cloudops.manager.operations.evidence.model.EvidenceFreshnessState;
import com.cloudops.manager.operations.evidence.model.EvidenceLifecycleRecord;
import com.cloudops.manager.operations.incident.model.IncidentRecord;
import com.cloudops.manager.operations.incident.model.IncidentSeverity;
import com.cloudops.manager.operations.incident.model.IncidentStatus;
import com.cloudops.manager.operations.incident.model.IncidentType;
import com.cloudops.manager.operations.risk.model.*;
import com.cloudops.manager.operations.risk.service.RiskCorrelationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RiskCorrelationEngineTest {

    private RiskCorrelationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RiskCorrelationEngine();
    }

    @Test
    @DisplayName("R001 & R002: Quota signals correctly trigger Capacity exhaustion risks")
    void testQuotaRiskRules() {
        ServiceQuotaItem ec2Critical = new ServiceQuotaItem(
                "ec2", "Amazon EC2", "L-1216C47A", "Running On-Demand Standard instances",
                32.0, 32.0, 100.0, QuotaStatus.CRITICAL, "ap-southeast-2", "EC2_VCPU_DISCOVERY", "vCPU", true, Instant.now()
        );

        ServiceQuotaItem vpcWarning = new ServiceQuotaItem(
                "vpc", "Amazon VPC", "L-F678F13E", "VPCs per Region",
                5.0, 4.0, 80.0, QuotaStatus.WARNING, "ap-southeast-2", "VPC_DISCOVERY", "Count", true, Instant.now()
        );

        List<OperationalRisk> risks = engine.correlate(
                "351405419700", "ap-southeast-2", List.of(ec2Critical, vpcWarning), List.of(), List.of()
        );

        assertEquals(2, risks.size());

        OperationalRisk r1 = risks.stream().filter(r -> "risk-quota-capacity-L-1216C47A".equals(r.riskId())).findFirst().orElseThrow();
        assertEquals(RiskCategory.CAPACITY, r1.category());
        assertEquals(RiskSeverity.CRITICAL, r1.severity());
        assertEquals(RiskSource.QUOTA, r1.sourceModule());
        assertEquals(ActionSafety.REQUIRES_APPROVAL, r1.action().safetyLevel());

        OperationalRisk r2 = risks.stream().filter(r -> "risk-quota-capacity-L-F678F13E".equals(r.riskId())).findFirst().orElseThrow();
        assertEquals(RiskCategory.CAPACITY, r2.category());
        assertEquals(RiskSeverity.HIGH, r2.severity());
    }

    @Test
    @DisplayName("R003-R007: Compliance failure signals trigger corresponding Security and Reliability risks")
    void testComplianceRiskRules() {
        ComplianceEvaluationResult openIngress = new ComplianceEvaluationResult(
                "SecSgOpenIngressRule", ComplianceCategory.SECURITY, ComplianceStatus.FAIL,
                "Security Groups Open Ingress", "Unrestricted SSH", List.of()
        );

        ComplianceEvaluationResult multiAz = new ComplianceEvaluationResult(
                "RelRdsMultiAzRule", ComplianceCategory.RELIABILITY, ComplianceStatus.FAIL,
                "RDS Multi-AZ", "Single AZ risk", List.of()
        );

        ComplianceEvaluationResult mfa = new ComplianceEvaluationResult(
                "SecIamMfaRule", ComplianceCategory.SECURITY, ComplianceStatus.FAIL,
                "IAM MFA Enforcement", "Credential compromise risk", List.of()
        );

        ComplianceEvaluationResult s3Public = new ComplianceEvaluationResult(
                "SecS3PublicAccessBlockRule", ComplianceCategory.SECURITY, ComplianceStatus.FAIL,
                "S3 Public Access Block", "Data leakage risk", List.of()
        );

        List<OperationalRisk> risks = engine.correlate(
                "351405419700", "ap-southeast-2", List.of(),
                List.of(openIngress, multiAz, mfa, s3Public), List.of()
        );

        assertEquals(4, risks.size());

        OperationalRisk rAdmin = risks.stream().filter(r -> r.riskId().contains("SecSgOpenIngressRule")).findFirst().orElseThrow();
        assertEquals(RiskSeverity.CRITICAL, rAdmin.severity());
        assertEquals(ActionSafety.HIGH_RISK, rAdmin.action().safetyLevel());

        OperationalRisk rRds = risks.stream().filter(r -> r.riskId().contains("RelRdsMultiAzRule")).findFirst().orElseThrow();
        assertEquals(RiskCategory.RELIABILITY, rRds.category());
        assertEquals(RiskSeverity.HIGH, rRds.severity());
    }

    @Test
    @DisplayName("R008 & Deduplication: Security exposure creates deterministic risk and avoids duplicates")
    void testSecurityExposureAndDeduplication() {
        SecurityExposureResult exposure = new SecurityExposureResult(
                "node-ec2-1", "EC2", "i-012345", ExposureStatus.EXPOSED,
                Map.of("publicIp", "3.107.26.181", "openPort", 8080), "351405419700", "ap-southeast-2"
        );

        List<OperationalRisk> run1 = engine.correlate(
                "351405419700", "ap-southeast-2", List.of(), List.of(), List.of(exposure)
        );
        List<OperationalRisk> run2 = engine.correlate(
                "351405419700", "ap-southeast-2", List.of(), List.of(), List.of(exposure, exposure)
        );

        assertEquals(1, run1.size());
        assertEquals(1, run2.size());
        assertEquals("risk-security-exposure-i-012345", run1.get(0).riskId());
        assertEquals(RiskSeverity.HIGH, run1.get(0).severity());
        assertEquals(RiskSource.SECURITY, run1.get(0).sourceModule());
    }

    @Test
    @DisplayName("R009 & R010: Drift signals trigger Security and Operational risks accurately")
    void testDriftRiskRules() {
        DriftResourceResult sgDrift = new DriftResourceResult(
                "aws_security_group.backend", "AWS::EC2::SecurityGroup", "sg-089b0f1",
                DriftStatus.DRIFTED, List.of(new DriftAttributeDifference("ingress", "8080", "22")),
                "Security group ingress drifted"
        );

        DriftResourceResult subnetNotFound = new DriftResourceResult(
                "aws_subnet.private_b", "AWS::EC2::Subnet", "subnet-missing-1",
                DriftStatus.NOT_FOUND, List.of(), "Subnet not found in live AWS"
        );

        List<OperationalRisk> risks = engine.correlate(
                "351405419700", "ap-southeast-2", List.of(), List.of(), List.of(),
                List.of(sgDrift, subnetNotFound), List.of(), List.of()
        );

        assertEquals(2, risks.size());

        OperationalRisk rSg = risks.stream().filter(r -> "risk-drift-security-sg-089b0f1".equals(r.riskId())).findFirst().orElseThrow();
        assertEquals(RiskCategory.SECURITY, rSg.category());
        assertEquals(RiskSeverity.HIGH, rSg.severity());
        assertEquals(RiskSource.DRIFT, rSg.sourceModule());
        assertEquals(ActionSafety.REQUIRES_APPROVAL, rSg.action().safetyLevel());

        OperationalRisk rMissing = risks.stream().filter(r -> "risk-drift-missing-aws_subnet.private_b".equals(r.riskId())).findFirst().orElseThrow();
        assertEquals(RiskCategory.OPERATIONAL, rMissing.category());
        assertEquals(RiskSeverity.CRITICAL, rMissing.severity());
        assertEquals(RiskSource.DRIFT, rMissing.sourceModule());
        assertEquals(ActionSafety.HIGH_RISK, rMissing.action().safetyLevel());
    }

    @Test
    @DisplayName("R011 Positive Cases: Active CRITICAL incidents with approved control-plane types trigger R011")
    void testR011PositiveCases() {
        // OPEN + CRITICAL + AWS_THROTTLED => R011
        IncidentRecord incThrottled = new IncidentRecord(
                "inc-throttled", IncidentType.AWS_THROTTLED, IncidentSeverity.CRITICAL,
                IncidentStatus.OPEN, "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), 5, "Throttled", "EC2", "ACTIVE", Map.of()
        );

        // DEGRADED + CRITICAL + AWS_TIMEOUT => R011
        IncidentRecord incTimeout = new IncidentRecord(
                "inc-timeout", IncidentType.AWS_TIMEOUT, IncidentSeverity.CRITICAL,
                IncidentStatus.DEGRADED, "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), 2, "Timeout", "RDS", "ACTIVE", Map.of()
        );

        // ACKNOWLEDGED + CRITICAL + AWS_ACCESS_DENIED => R011
        IncidentRecord incAccessDenied = new IncidentRecord(
                "inc-access-denied", IncidentType.AWS_ACCESS_DENIED, IncidentSeverity.CRITICAL,
                IncidentStatus.ACKNOWLEDGED, "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), 3, "Access denied", "IAM", "ACTIVE", Map.of()
        );

        // OPEN + CRITICAL + CIRCUIT_BREAKER_OPEN => R011
        IncidentRecord incCircuitBreaker = new IncidentRecord(
                "inc-circuit-breaker", IncidentType.CIRCUIT_BREAKER_OPEN, IncidentSeverity.CRITICAL,
                IncidentStatus.OPEN, "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), 1, "Circuit breaker tripped", "CORE", "ACTIVE", Map.of()
        );

        List<OperationalRisk> risks = engine.correlate(
                "351405419700", "ap-southeast-2", List.of(), List.of(), List.of(),
                List.of(), List.of(incThrottled, incTimeout, incAccessDenied, incCircuitBreaker), List.of()
        );

        assertEquals(4, risks.size());

        for (OperationalRisk r : risks) {
            assertEquals(RiskCategory.OPERATIONAL, r.category());
            assertEquals(RiskSeverity.CRITICAL, r.severity());
            assertEquals(RiskSource.RESILIENCE, r.sourceModule());
            assertEquals(ActionSafety.READ_ONLY, r.action().safetyLevel());
            assertTrue(r.riskId().startsWith("risk-resilience-incident-"));
        }

        assertTrue(risks.stream().anyMatch(r -> "risk-resilience-incident-inc-throttled".equals(r.riskId())));
        assertTrue(risks.stream().anyMatch(r -> "risk-resilience-incident-inc-timeout".equals(r.riskId())));
        assertTrue(risks.stream().anyMatch(r -> "risk-resilience-incident-inc-access-denied".equals(r.riskId())));
        assertTrue(risks.stream().anyMatch(r -> "risk-resilience-incident-inc-circuit-breaker".equals(r.riskId())));
    }

    @Test
    @DisplayName("R011 Negative Cases: Excluded incident types, non-critical severity, and inactive states produce NO R011")
    void testR011NegativeCases() {
        // Excluded types: AWS_UNAVAILABLE, DISCOVERY_DEGRADED, SYSTEM_DEGRADED
        IncidentRecord incUnavailable = new IncidentRecord(
                "inc-unavail", IncidentType.AWS_UNAVAILABLE, IncidentSeverity.CRITICAL,
                IncidentStatus.OPEN, "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), 1, "Unavailable", "EC2", "ACTIVE", Map.of()
        );

        IncidentRecord incDiscoveryDegraded = new IncidentRecord(
                "inc-disc-deg", IncidentType.DISCOVERY_DEGRADED, IncidentSeverity.CRITICAL,
                IncidentStatus.OPEN, "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), 1, "Discovery degraded", "CORE", "ACTIVE", Map.of()
        );

        IncidentRecord incSystemDegraded = new IncidentRecord(
                "inc-sys-deg", IncidentType.SYSTEM_DEGRADED, IncidentSeverity.CRITICAL,
                IncidentStatus.OPEN, "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), 1, "System degraded", "CORE", "ACTIVE", Map.of()
        );

        // Non-critical severity: OPEN + WARNING + AWS_THROTTLED
        IncidentRecord incWarningThrottled = new IncidentRecord(
                "inc-warn-throttled", IncidentType.AWS_THROTTLED, IncidentSeverity.WARNING,
                IncidentStatus.OPEN, "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), 1, "Warn throttled", "EC2", "ACTIVE", Map.of()
        );

        // Inactive states: RESOLVED + CRITICAL + AWS_THROTTLED, RECOVERING + CRITICAL + AWS_TIMEOUT
        IncidentRecord incResolved = new IncidentRecord(
                "inc-resolved", IncidentType.AWS_THROTTLED, IncidentSeverity.CRITICAL,
                IncidentStatus.RESOLVED, "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), 1, "Resolved", "EC2", "RESOLVED", Map.of()
        );

        IncidentRecord incRecovering = new IncidentRecord(
                "inc-recovering", IncidentType.AWS_TIMEOUT, IncidentSeverity.CRITICAL,
                IncidentStatus.RECOVERING, "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), 1, "Recovering", "RDS", "RECOVERING", Map.of()
        );

        List<OperationalRisk> risks = engine.correlate(
                "351405419700", "ap-southeast-2", List.of(), List.of(), List.of(),
                List.of(), List.of(incUnavailable, incDiscoveryDegraded, incSystemDegraded, incWarningThrottled, incResolved, incRecovering), List.of()
        );

        assertEquals(0, risks.size(), "Excluded incident types, non-critical severities, and inactive statuses must produce 0 risks");
    }

    @Test
    @DisplayName("R012 & Freshness Semantics: Expired evidence triggers MEDIUM risk; FRESH/AGING/UNAVAILABLE produce no risk")
    void testEvidenceFreshnessRiskRules() {
        EvidenceLifecycleRecord expired = new EvidenceLifecycleRecord(
                "DISCOVERY", "351405419700", "ap-southeast-2",
                Instant.now().minusSeconds(4000), Instant.now().minusSeconds(4000), Instant.now(),
                4000, EvidenceFreshnessState.EXPIRED, "digest-1"
        );

        EvidenceLifecycleRecord fresh = new EvidenceLifecycleRecord(
                "SECURITY", "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), Instant.now(),
                120, EvidenceFreshnessState.FRESH, "digest-2"
        );

        EvidenceLifecycleRecord aging = new EvidenceLifecycleRecord(
                "COMPLIANCE", "351405419700", "ap-southeast-2",
                Instant.now().minusSeconds(600), Instant.now().minusSeconds(600), Instant.now(),
                600, EvidenceFreshnessState.AGING, "digest-3"
        );

        EvidenceLifecycleRecord unavailable = new EvidenceLifecycleRecord(
                "TOPOLOGY", "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), Instant.now(),
                0, EvidenceFreshnessState.UNAVAILABLE, "digest-4"
        );

        List<OperationalRisk> risks = engine.correlate(
                "351405419700", "ap-southeast-2", List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(expired, fresh, aging, unavailable)
        );

        assertEquals(1, risks.size());
        OperationalRisk rExpired = risks.get(0);
        assertEquals("risk-resilience-evidence-DISCOVERY", rExpired.riskId());
        assertEquals(RiskCategory.OPERATIONAL, rExpired.category());
        assertEquals(RiskSeverity.MEDIUM, rExpired.severity());
        assertEquals(RiskSource.RESILIENCE, rExpired.sourceModule());
        assertEquals(ActionSafety.READ_ONLY, rExpired.action().safetyLevel());
    }

    @Test
    @DisplayName("Deduplication: Repeated incidents or drift findings yield deterministic stable risk IDs")
    void testDeduplicationStability() {
        IncidentRecord inc = new IncidentRecord(
                "inc-throttled-repeat", IncidentType.AWS_THROTTLED, IncidentSeverity.CRITICAL,
                IncidentStatus.OPEN, "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), 5, "Throttled", "EC2", "ACTIVE", Map.of()
        );

        List<OperationalRisk> scan1 = engine.correlate(
                "351405419700", "ap-southeast-2", List.of(), List.of(), List.of(),
                List.of(), List.of(inc), List.of()
        );

        List<OperationalRisk> scan2 = engine.correlate(
                "351405419700", "ap-southeast-2", List.of(), List.of(), List.of(),
                List.of(), List.of(inc, inc), List.of()
        );

        assertEquals(1, scan1.size());
        assertEquals(1, scan2.size());
        assertEquals("risk-resilience-incident-inc-throttled-repeat", scan1.get(0).riskId());
        assertEquals("risk-resilience-incident-inc-throttled-repeat", scan2.get(0).riskId());
    }
}
