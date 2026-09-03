package com.cloudops.manager.operations.risk.service;

import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationReport;
import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationResult;
import com.cloudops.manager.aws.compliance.service.ComplianceEvaluationService;
import com.cloudops.manager.aws.drift.model.DriftResourceResult;
import com.cloudops.manager.aws.quota.model.QuotaUtilizationReport;
import com.cloudops.manager.aws.quota.model.ServiceQuotaItem;
import com.cloudops.manager.aws.quota.service.ServiceQuotasService;
import com.cloudops.manager.aws.security.model.SecurityExposureResult;
import com.cloudops.manager.aws.security.service.SecurityAnalysisService;
import com.cloudops.manager.aws.sts.provider.StsIdentityProvider;
import com.cloudops.manager.operations.evidence.model.EvidenceLifecycleRecord;
import com.cloudops.manager.operations.evidence.service.EvidenceLifecycleService;
import com.cloudops.manager.operations.incident.model.IncidentRecord;
import com.cloudops.manager.operations.incident.service.IncidentManagementService;
import com.cloudops.manager.operations.risk.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class RiskAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentService.class);

    private final ServiceQuotasService serviceQuotasService;
    private final ComplianceEvaluationService complianceEvaluationService;
    private final SecurityAnalysisService securityAnalysisService;
    private final IncidentManagementService incidentManagementService;
    private final EvidenceLifecycleService evidenceLifecycleService;
    private final StsIdentityProvider stsIdentityProvider;
    private final RiskCorrelationEngine correlationEngine;
    private final String defaultRegion;

    public RiskAssessmentService(
            ServiceQuotasService serviceQuotasService,
            ComplianceEvaluationService complianceEvaluationService,
            SecurityAnalysisService securityAnalysisService,
            IncidentManagementService incidentManagementService,
            EvidenceLifecycleService evidenceLifecycleService,
            StsIdentityProvider stsIdentityProvider,
            RiskCorrelationEngine correlationEngine,
            @Value("${cloudops.aws.region:ap-southeast-2}") String defaultRegion
    ) {
        this.serviceQuotasService = serviceQuotasService;
        this.complianceEvaluationService = complianceEvaluationService;
        this.securityAnalysisService = securityAnalysisService;
        this.incidentManagementService = incidentManagementService;
        this.evidenceLifecycleService = evidenceLifecycleService;
        this.stsIdentityProvider = stsIdentityProvider;
        this.correlationEngine = correlationEngine;
        this.defaultRegion = defaultRegion;
    }

    public RiskAssessmentReport getRiskAssessment(String optionalRegion, String optionalCategory, String optionalSeverity) {
        String region = (optionalRegion != null && !optionalRegion.isBlank()) ? optionalRegion.trim() : defaultRegion;
        String accountId = resolveAccountId();

        log.info("Generating operational risk assessment for account: {}, region: {}", accountId, region);

        // 1. Ingest Quota Signals (with partial failure resilience)
        List<ServiceQuotaItem> quotaItems = new ArrayList<>();
        try {
            QuotaUtilizationReport quotaReport = serviceQuotasService.getQuotaUtilizationReport(region);
            if (quotaReport != null && quotaReport.quotas() != null) {
                quotaItems = quotaReport.quotas();
            }
        } catch (Exception e) {
            log.warn("Non-fatal error ingesting quota signals for risk correlation in region {}: {}", region, e.getMessage());
        }

        // 2. Ingest Compliance Signals (with partial failure resilience)
        List<ComplianceEvaluationResult> complianceResults = new ArrayList<>();
        try {
            ComplianceEvaluationReport complianceReport = complianceEvaluationService.evaluateLocal(region, null);
            if (complianceReport != null && complianceReport.results() != null) {
                complianceResults = complianceReport.results();
            }
        } catch (Exception e) {
            log.warn("Non-fatal error ingesting compliance signals for risk correlation in region {}: {}", region, e.getMessage());
        }

        // 3. Ingest Security Signals (with partial failure resilience)
        List<SecurityExposureResult> securityExposures = new ArrayList<>();
        try {
            List<SecurityExposureResult> exposures = securityAnalysisService.getExposures(region);
            if (exposures != null) {
                securityExposures = exposures;
            }
        } catch (Exception e) {
            log.warn("Non-fatal error ingesting security exposures for risk correlation in region {}: {}", region, e.getMessage());
        }

        // 4. Ingest Drift Signals (if available)
        List<DriftResourceResult> driftResults = new ArrayList<>();

        // 5. Ingest Active Incidents (with partial failure resilience)
        List<IncidentRecord> activeIncidents = new ArrayList<>();
        try {
            List<IncidentRecord> incidents = incidentManagementService.getActiveIncidents();
            if (incidents != null) {
                activeIncidents = incidents;
            }
        } catch (Exception e) {
            log.warn("Non-fatal error ingesting active incidents for risk correlation: {}", e.getMessage());
        }

        // 6. Ingest Evidence Lifecycle States (with partial failure resilience)
        List<EvidenceLifecycleRecord> evidenceStates = new ArrayList<>();
        try {
            List<EvidenceLifecycleRecord> lifecycles = evidenceLifecycleService.getEvidenceLifecycles(accountId, region);
            if (lifecycles != null) {
                evidenceStates = lifecycles;
            }
        } catch (Exception e) {
            log.warn("Non-fatal error ingesting evidence lifecycles for risk correlation: {}", e.getMessage());
        }

        // Correlate into unified risks
        List<OperationalRisk> allRisks = correlationEngine.correlate(
                accountId, region, quotaItems, complianceResults, securityExposures, driftResults, activeIncidents, evidenceStates
        );

        // Apply optional category & severity filtering
        List<OperationalRisk> filteredRisks = allRisks.stream()
                .filter(r -> {
                    if (optionalCategory != null && !optionalCategory.isBlank()) {
                        return r.category().name().equalsIgnoreCase(optionalCategory.trim());
                    }
                    return true;
                })
                .filter(r -> {
                    if (optionalSeverity != null && !optionalSeverity.isBlank()) {
                        return r.severity().name().equalsIgnoreCase(optionalSeverity.trim());
                    }
                    return true;
                })
                .toList();

        int criticalCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        int lowCount = 0;

        for (OperationalRisk r : filteredRisks) {
            switch (r.severity()) {
                case CRITICAL -> criticalCount++;
                case HIGH -> highCount++;
                case MEDIUM -> mediumCount++;
                case LOW -> lowCount++;
            }
        }

        return new RiskAssessmentReport(
                accountId,
                region,
                filteredRisks.size(),
                criticalCount,
                highCount,
                mediumCount,
                lowCount,
                filteredRisks,
                Instant.now()
        );
    }

    private String resolveAccountId() {
        try {
            return stsIdentityProvider.getCallerIdentity().accountId();
        } catch (Exception e) {
            log.warn("Failed to resolve AWS STS caller identity for risk assessment: {}", e.getMessage());
            return "351405419700";
        }
    }
}
