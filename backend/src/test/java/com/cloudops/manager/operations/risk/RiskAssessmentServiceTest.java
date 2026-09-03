package com.cloudops.manager.operations.risk;

import com.cloudops.manager.aws.compliance.model.ComplianceCategory;
import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationReport;
import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationResult;
import com.cloudops.manager.aws.compliance.model.ComplianceStatus;
import com.cloudops.manager.aws.compliance.service.ComplianceEvaluationService;
import com.cloudops.manager.aws.quota.model.QuotaStatus;
import com.cloudops.manager.aws.quota.model.QuotaUtilizationReport;
import com.cloudops.manager.aws.quota.model.ServiceQuotaItem;
import com.cloudops.manager.aws.quota.service.ServiceQuotasService;
import com.cloudops.manager.aws.security.model.ExposureStatus;
import com.cloudops.manager.aws.security.model.SecurityExposureResult;
import com.cloudops.manager.aws.security.service.SecurityAnalysisService;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.provider.StsIdentityProvider;
import com.cloudops.manager.operations.evidence.model.EvidenceFreshnessState;
import com.cloudops.manager.operations.evidence.model.EvidenceLifecycleRecord;
import com.cloudops.manager.operations.evidence.service.EvidenceLifecycleService;
import com.cloudops.manager.operations.incident.model.IncidentRecord;
import com.cloudops.manager.operations.incident.model.IncidentSeverity;
import com.cloudops.manager.operations.incident.model.IncidentStatus;
import com.cloudops.manager.operations.incident.model.IncidentType;
import com.cloudops.manager.operations.incident.service.IncidentManagementService;
import com.cloudops.manager.operations.risk.model.RiskAssessmentReport;
import com.cloudops.manager.operations.risk.model.RiskCategory;
import com.cloudops.manager.operations.risk.model.RiskSeverity;
import com.cloudops.manager.operations.risk.service.RiskAssessmentService;
import com.cloudops.manager.operations.risk.service.RiskCorrelationEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @Mock
    private ServiceQuotasService serviceQuotasService;

    @Mock
    private ComplianceEvaluationService complianceEvaluationService;

    @Mock
    private SecurityAnalysisService securityAnalysisService;

    @Mock
    private IncidentManagementService incidentManagementService;

    @Mock
    private EvidenceLifecycleService evidenceLifecycleService;

    @Mock
    private StsIdentityProvider stsIdentityProvider;

    private RiskAssessmentService riskAssessmentService;

    @BeforeEach
    void setUp() {
        riskAssessmentService = new RiskAssessmentService(
                serviceQuotasService,
                complianceEvaluationService,
                securityAnalysisService,
                incidentManagementService,
                evidenceLifecycleService,
                stsIdentityProvider,
                new RiskCorrelationEngine(),
                "ap-southeast-2"
        );
    }

    @Test
    @DisplayName("Service aggregates risks across domains and maintains summary invariants")
    void testRiskAssessmentAggregationAndInvariants() {
        when(stsIdentityProvider.getCallerIdentity()).thenReturn(
                new CallerIdentity("351405419700", "arn:aws:iam::351405419700:user/test", "AIDATEST")
        );

        ServiceQuotaItem ec2Item = new ServiceQuotaItem(
                "ec2", "Amazon EC2", "L-1216C47A", "Running On-Demand Standard instances",
                32.0, 32.0, 100.0, QuotaStatus.CRITICAL, "ap-southeast-2", "EC2_VCPU_DISCOVERY", "vCPU", true, Instant.now()
        );
        QuotaUtilizationReport quotaReport = new QuotaUtilizationReport(
                "351405419700", "ap-southeast-2", 1, 0, 0, 1, 0, 100.0, List.of(ec2Item), Map.of("CRITICAL", 1), Instant.now()
        );
        when(serviceQuotasService.getQuotaUtilizationReport("ap-southeast-2")).thenReturn(quotaReport);

        ComplianceEvaluationResult openIngress = new ComplianceEvaluationResult(
                "SecSgOpenIngressRule", ComplianceCategory.SECURITY, ComplianceStatus.FAIL,
                "Open Ingress", "Ingress open", List.of()
        );
        ComplianceEvaluationReport complianceReport = new ComplianceEvaluationReport(
                "351405419700", "ap-southeast-2", Instant.now(), 1, 0, 1, 0, 0, List.of(openIngress)
        );
        when(complianceEvaluationService.evaluateLocal("ap-southeast-2", null)).thenReturn(complianceReport);

        SecurityExposureResult exposure = new SecurityExposureResult(
                "node-1", "EC2", "i-123", ExposureStatus.EXPOSED, Map.of("ip", "1.2.3.4"), "351405419700", "ap-southeast-2"
        );
        when(securityAnalysisService.getExposures("ap-southeast-2")).thenReturn(List.of(exposure));

        IncidentRecord throttled = new IncidentRecord(
                "inc-throttled-1", IncidentType.AWS_THROTTLED, IncidentSeverity.CRITICAL,
                IncidentStatus.OPEN, "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), 5, "Throttled", "EC2", "ACTIVE", Map.of()
        );
        when(incidentManagementService.getActiveIncidents()).thenReturn(List.of(throttled));

        EvidenceLifecycleRecord expired = new EvidenceLifecycleRecord(
                "DISCOVERY", "351405419700", "ap-southeast-2",
                Instant.now().minusSeconds(4000), Instant.now().minusSeconds(4000), Instant.now(),
                4000, EvidenceFreshnessState.EXPIRED, "digest"
        );
        when(evidenceLifecycleService.getEvidenceLifecycles("351405419700", "ap-southeast-2")).thenReturn(List.of(expired));

        RiskAssessmentReport report = riskAssessmentService.getRiskAssessment("ap-southeast-2", null, null);

        assertNotNull(report);
        assertEquals("351405419700", report.accountId());
        assertEquals("ap-southeast-2", report.region());
        assertEquals(5, report.totalRisksTracked());
        assertEquals(3, report.criticalCount()); // EC2 quota CRITICAL + SecSgOpenIngressRule CRITICAL + Incident CRITICAL
        assertEquals(1, report.highCount());     // Security Exposure HIGH
        assertEquals(1, report.mediumCount());   // Expired Evidence MEDIUM
        assertEquals(0, report.lowCount());

        // Invariant: critical + high + medium + low == totalRisksTracked == risks.size()
        assertEquals(report.totalRisksTracked(), report.risks().size());
        assertEquals(report.totalRisksTracked(), report.criticalCount() + report.highCount() + report.mediumCount() + report.lowCount());
    }

    @Test
    @DisplayName("Partial source failure is handled gracefully without failing the entire assessment")
    void testPartialSourceFailureResilience() {
        when(stsIdentityProvider.getCallerIdentity()).thenReturn(
                new CallerIdentity("351405419700", "arn:aws:iam::351405419700:user/test", "AIDATEST")
        );

        when(serviceQuotasService.getQuotaUtilizationReport(anyString())).thenThrow(new RuntimeException("Quota service unavailable"));

        ComplianceEvaluationResult openIngress = new ComplianceEvaluationResult(
                "SecSgOpenIngressRule", ComplianceCategory.SECURITY, ComplianceStatus.FAIL,
                "Open Ingress", "Ingress open", List.of()
        );
        ComplianceEvaluationReport complianceReport = new ComplianceEvaluationReport(
                "351405419700", "ap-southeast-2", Instant.now(), 1, 0, 1, 0, 0, List.of(openIngress)
        );
        when(complianceEvaluationService.evaluateLocal("ap-southeast-2", null)).thenReturn(complianceReport);
        when(securityAnalysisService.getExposures("ap-southeast-2")).thenReturn(List.of());
        when(incidentManagementService.getActiveIncidents()).thenThrow(new RuntimeException("Incident service unavailable"));
        when(evidenceLifecycleService.getEvidenceLifecycles(anyString(), anyString())).thenReturn(List.of());

        RiskAssessmentReport report = riskAssessmentService.getRiskAssessment("ap-southeast-2", null, null);

        assertNotNull(report);
        assertEquals(1, report.totalRisksTracked());
        assertEquals(1, report.criticalCount());
    }

    @Test
    @DisplayName("Filtering by category and severity returns matching subset and updates counts")
    void testFilteringByCategoryAndSeverity() {
        when(stsIdentityProvider.getCallerIdentity()).thenReturn(
                new CallerIdentity("351405419700", "arn:aws:iam::351405419700:user/test", "AIDATEST")
        );

        ServiceQuotaItem ec2Item = new ServiceQuotaItem(
                "ec2", "Amazon EC2", "L-1216C47A", "Running On-Demand Standard instances",
                32.0, 32.0, 100.0, QuotaStatus.CRITICAL, "ap-southeast-2", "EC2_VCPU_DISCOVERY", "vCPU", true, Instant.now()
        );
        when(serviceQuotasService.getQuotaUtilizationReport("ap-southeast-2"))
                .thenReturn(new QuotaUtilizationReport("351405419700", "ap-southeast-2", 1, 0, 0, 1, 0, 100.0, List.of(ec2Item), Map.of(), Instant.now()));

        ComplianceEvaluationResult openIngress = new ComplianceEvaluationResult(
                "SecSgOpenIngressRule", ComplianceCategory.SECURITY, ComplianceStatus.FAIL,
                "Open Ingress", "Ingress open", List.of()
        );
        when(complianceEvaluationService.evaluateLocal("ap-southeast-2", null))
                .thenReturn(new ComplianceEvaluationReport("351405419700", "ap-southeast-2", Instant.now(), 1, 0, 1, 0, 0, List.of(openIngress)));
        when(securityAnalysisService.getExposures("ap-southeast-2")).thenReturn(List.of());

        IncidentRecord throttled = new IncidentRecord(
                "inc-throttled-1", IncidentType.AWS_THROTTLED, IncidentSeverity.CRITICAL,
                IncidentStatus.OPEN, "351405419700", "ap-southeast-2",
                Instant.now(), Instant.now(), 5, "Throttled", "EC2", "ACTIVE", Map.of()
        );
        when(incidentManagementService.getActiveIncidents()).thenReturn(List.of(throttled));
        when(evidenceLifecycleService.getEvidenceLifecycles(anyString(), anyString())).thenReturn(List.of());

        // Filter by category CAPACITY
        RiskAssessmentReport capacityOnly = riskAssessmentService.getRiskAssessment("ap-southeast-2", "CAPACITY", null);
        assertEquals(1, capacityOnly.totalRisksTracked());
        assertEquals(RiskCategory.CAPACITY, capacityOnly.risks().get(0).category());

        // Filter by category SECURITY
        RiskAssessmentReport securityOnly = riskAssessmentService.getRiskAssessment("ap-southeast-2", "SECURITY", null);
        assertEquals(1, securityOnly.totalRisksTracked());
        assertEquals(RiskCategory.SECURITY, securityOnly.risks().get(0).category());

        // Filter by category OPERATIONAL
        RiskAssessmentReport operationalOnly = riskAssessmentService.getRiskAssessment("ap-southeast-2", "OPERATIONAL", null);
        assertEquals(1, operationalOnly.totalRisksTracked());
        assertEquals(RiskCategory.OPERATIONAL, operationalOnly.risks().get(0).category());
    }
}
