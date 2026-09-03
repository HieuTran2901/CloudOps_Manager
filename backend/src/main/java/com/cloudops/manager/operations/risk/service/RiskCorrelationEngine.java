package com.cloudops.manager.operations.risk.service;

import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationResult;
import com.cloudops.manager.aws.compliance.model.ComplianceStatus;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Deterministic correlation engine evaluating domain signals against operational risk rules (R001 - R012).
 */
@Component
public class RiskCorrelationEngine {

    private static final Logger log = LoggerFactory.getLogger(RiskCorrelationEngine.class);

    private static final Set<IncidentType> CONTROL_PLANE_INCIDENT_TYPES = Set.of(
            IncidentType.AWS_THROTTLED,
            IncidentType.AWS_TIMEOUT,
            IncidentType.AWS_ACCESS_DENIED,
            IncidentType.CIRCUIT_BREAKER_OPEN
    );

    public List<OperationalRisk> correlate(
            String accountId,
            String region,
            List<ServiceQuotaItem> quotaItems,
            List<ComplianceEvaluationResult> complianceResults,
            List<SecurityExposureResult> securityExposures
    ) {
        return correlate(accountId, region, quotaItems, complianceResults, securityExposures, List.of(), List.of(), List.of());
    }

    public List<OperationalRisk> correlate(
            String accountId,
            String region,
            List<ServiceQuotaItem> quotaItems,
            List<ComplianceEvaluationResult> complianceResults,
            List<SecurityExposureResult> securityExposures,
            List<DriftResourceResult> driftResults,
            List<IncidentRecord> activeIncidents,
            List<EvidenceLifecycleRecord> evidenceStates
    ) {
        log.info("Correlating operational risks for account: {}, region: {}", accountId, region);
        Map<String, OperationalRisk> riskMap = new LinkedHashMap<>();
        Instant detectedAt = Instant.now();

        // 1. Evaluate Quota Signals (R001, R002)
        if (quotaItems != null) {
            for (ServiceQuotaItem q : quotaItems) {
                if (q == null) continue;

                // R001: EC2 Capacity Exhaustion
                if ("ec2".equalsIgnoreCase(q.serviceCode()) && "L-1216C47A".equalsIgnoreCase(q.quotaCode()) && q.status() == QuotaStatus.CRITICAL) {
                    String riskId = "risk-quota-capacity-L-1216C47A";
                    Map<String, Object> evidence = new LinkedHashMap<>();
                    evidence.put("quotaCode", q.quotaCode());
                    evidence.put("currentUsage", q.currentUsage());
                    evidence.put("appliedLimit", q.appliedLimit());
                    evidence.put("utilizationPercentage", q.utilizationPercentage());
                    evidence.put("usageSource", q.usageSource());

                    RecommendedAction action = new RecommendedAction(
                            "request-ec2-vcpu-quota-increase",
                            "Request EC2 vCPU quota increase",
                            "Request an applied limit increase for On-Demand Standard vCPUs via AWS Service Quotas or scale down idle compute capacity.",
                            ActionSafety.REQUIRES_APPROVAL,
                            List.of(
                                    "Navigate to AWS Service Quotas console in " + region,
                                    "Select EC2 -> Running On-Demand Standard instances",
                                    "Submit request for quota increase or terminate non-essential instances"
                            ),
                            "Verify vCPU utilization drops below 80% on /api/v1/quotas"
                    );

                    riskMap.put(riskId, new OperationalRisk(
                            riskId,
                            RiskCategory.CAPACITY,
                            RiskSeverity.CRITICAL,
                            "EC2 capacity exhaustion risk",
                            "EC2 vCPU usage has reached critical capacity (" + q.utilizationPercentage() + "%). New instances or auto-scaling scaling events will fail.",
                            "Scaling events and instance provisioning blocked, causing operational degradation.",
                            List.of(q.quotaCode()),
                            evidence,
                            detectedAt,
                            action,
                            RiskSource.QUOTA
                    ));
                }
                // R002: General Quota Capacity Depletion
                else if (q.status() == QuotaStatus.WARNING) {
                    String riskId = "risk-quota-capacity-" + q.quotaCode();
                    Map<String, Object> evidence = new LinkedHashMap<>();
                    evidence.put("quotaCode", q.quotaCode());
                    evidence.put("currentUsage", q.currentUsage());
                    evidence.put("appliedLimit", q.appliedLimit());
                    evidence.put("utilizationPercentage", q.utilizationPercentage());
                    evidence.put("serviceCode", q.serviceCode());

                    RecommendedAction action = new RecommendedAction(
                            "plan-quota-capacity-increase-" + q.quotaCode().toLowerCase(),
                            "Plan capacity increase for " + q.quotaName(),
                            "Monitor resource growth trajectory and initiate quota expansion before reaching 90% threshold.",
                            ActionSafety.REQUIRES_APPROVAL,
                            List.of(
                                    "Audit active resources for service " + q.serviceCode(),
                                    "Submit quota increase buffer request if growth is expected"
                            ),
                            "Verify quota status remains WARNING or improves to NORMAL"
                    );

                    riskMap.put(riskId, new OperationalRisk(
                            riskId,
                            RiskCategory.CAPACITY,
                            RiskSeverity.HIGH,
                            "Quota capacity depletion: " + q.quotaName(),
                            "Resource consumption has exceeded 80% (" + q.utilizationPercentage() + "% of limit " + q.appliedLimit() + ").",
                            "Approaching hard AWS capacity ceiling; risk of provisioning failure during traffic spikes.",
                            List.of(q.quotaCode()),
                            evidence,
                            detectedAt,
                            action,
                            RiskSource.QUOTA
                    ));
                }
            }
        }

        // 2. Evaluate Compliance Signals (R003, R004, R005, R006, R007)
        if (complianceResults != null) {
            for (ComplianceEvaluationResult cr : complianceResults) {
                if (cr == null || cr.status() != ComplianceStatus.FAIL) continue;

                String ruleId = cr.ruleId();

                // R003: Public Administrative Exposure (SSH/RDP open)
                if ("SecSgOpenIngressRule".equalsIgnoreCase(ruleId)) {
                    String riskId = "risk-compliance-security-SecSgOpenIngressRule";
                    Map<String, Object> evidence = new LinkedHashMap<>();
                    evidence.put("ruleId", ruleId);
                    evidence.put("evidenceCount", cr.evidence() != null ? cr.evidence().size() : 1);
                    evidence.put("explanation", cr.explanation());

                    RecommendedAction action = new RecommendedAction(
                            "restrict-admin-ingress",
                            "Restrict administrative ingress to trusted CIDR / bastion",
                            "Revoke wildcard 0.0.0.0/0 inbound rules on management ports (TCP 22, 3389) and restrict to VPN or bastion security group.",
                            ActionSafety.HIGH_RISK,
                            List.of(
                                    "Identify affected Security Groups from compliance report",
                                    "Replace 0.0.0.0/0 ingress rule with corporate VPN CIDR or bastion SG ID",
                                    "Verify active SSH/RDP sessions are not abruptly terminated"
                            ),
                            "Run compliance evaluation to confirm SecSgOpenIngressRule passes"
                    );

                    riskMap.put(riskId, new OperationalRisk(
                            riskId,
                            RiskCategory.SECURITY,
                            RiskSeverity.CRITICAL,
                            "Public administrative exposure detected",
                            "One or more security groups allow unrestricted public inbound access (0.0.0.0/0) on administrative ports (SSH/RDP).",
                            "Severe attack surface exposure enabling brute-force and remote exploit attempts.",
                            List.of("SecSgOpenIngressRule"),
                            evidence,
                            detectedAt,
                            action,
                            RiskSource.COMPLIANCE
                    ));
                }
                // R004: Public EC2 with Admin Activity / Exposure
                else if ("SecEc2PublicAdminExposureRule".equalsIgnoreCase(ruleId) || "SecEc2AdminActivityCorrelationRule".equalsIgnoreCase(ruleId)) {
                    String riskId = "risk-compliance-security-" + ruleId;
                    Map<String, Object> evidence = new LinkedHashMap<>();
                    evidence.put("ruleId", ruleId);
                    evidence.put("explanation", cr.explanation());

                    RecommendedAction action = new RecommendedAction(
                            "isolate-public-admin-node",
                            "Isolate public node with administrative privileges",
                            "Remove public IP association or restrict IAM role permissions attached to publicly reachable compute instances.",
                            ActionSafety.REQUIRES_APPROVAL,
                            List.of(
                                    "Move public instance behind Application Load Balancer in private subnet",
                                    "Strip administrative IAM permissions from instance profile"
                            ),
                            "Verify instance has no direct public IP and rule evaluation passes"
                    );

                    riskMap.put(riskId, new OperationalRisk(
                            riskId,
                            RiskCategory.SECURITY,
                            RiskSeverity.CRITICAL,
                            "Public compute node with elevated administrative exposure",
                            "Publicly reachable EC2 instance exhibits high administrative privileges or sensitive access.",
                            "Potential initial compromise vector leading directly to AWS account takeover.",
                            List.of(ruleId),
                            evidence,
                            detectedAt,
                            action,
                            RiskSource.COMPLIANCE
                    ));
                }
                // R005: Single-AZ Production Database
                else if ("RelRdsMultiAzRule".equalsIgnoreCase(ruleId)) {
                    String riskId = "risk-compliance-reliability-RelRdsMultiAzRule";
                    Map<String, Object> evidence = new LinkedHashMap<>();
                    evidence.put("ruleId", ruleId);
                    evidence.put("explanation", cr.explanation());

                    RecommendedAction action = new RecommendedAction(
                            "enable-rds-multi-az",
                            "Enable RDS Multi-AZ replication",
                            "Modify RDS database instance configuration to enable Multi-AZ deployment for automatic failover.",
                            ActionSafety.REQUIRES_APPROVAL,
                            List.of(
                                    "Review RDS maintenance window and storage IOPS",
                                    "Enable Multi-AZ flag on target DB instance in RDS console",
                                    "Monitor standby replica synchronization"
                            ),
                            "Verify RDS instance multiAZ attribute is true"
                    );

                    riskMap.put(riskId, new OperationalRisk(
                            riskId,
                            RiskCategory.RELIABILITY,
                            RiskSeverity.HIGH,
                            "Single-AZ database high availability risk",
                            "Relational Database (RDS) instance is deployed in a single Availability Zone without automatic failover.",
                            "Loss of primary AZ will cause extended database downtime and service disruption.",
                            List.of("RelRdsMultiAzRule"),
                            evidence,
                            detectedAt,
                            action,
                            RiskSource.COMPLIANCE
                    ));
                }
                // R006: IAM MFA Missing
                else if ("SecIamMfaRule".equalsIgnoreCase(ruleId)) {
                    String riskId = "risk-compliance-security-SecIamMfaRule";
                    Map<String, Object> evidence = new LinkedHashMap<>();
                    evidence.put("ruleId", ruleId);
                    evidence.put("explanation", cr.explanation());

                    RecommendedAction action = new RecommendedAction(
                            "enforce-iam-mfa",
                            "Enforce Multi-Factor Authentication (MFA)",
                            "Enable virtual MFA device for all active IAM console users.",
                            ActionSafety.REQUIRES_APPROVAL,
                            List.of(
                                    "Identify IAM users without MFA active",
                                    "Require MFA device registration at next login"
                            ),
                            "Verify MFA status on /api/v1/compliance"
                    );

                    riskMap.put(riskId, new OperationalRisk(
                            riskId,
                            RiskCategory.SECURITY,
                            RiskSeverity.HIGH,
                            "IAM users missing Multi-Factor Authentication",
                            "One or more active IAM user accounts do not have MFA enforced.",
                            "Heightened risk of credential theft and unauthorized access via compromised passwords.",
                            List.of("SecIamMfaRule"),
                            evidence,
                            detectedAt,
                            action,
                            RiskSource.COMPLIANCE
                    ));
                }
                // R007: S3 Public Access Control Gap
                else if ("SecS3PublicAccessBlockRule".equalsIgnoreCase(ruleId)) {
                    String riskId = "risk-compliance-security-SecS3PublicAccessBlockRule";
                    Map<String, Object> evidence = new LinkedHashMap<>();
                    evidence.put("ruleId", ruleId);
                    evidence.put("explanation", cr.explanation());

                    RecommendedAction action = new RecommendedAction(
                            "enable-s3-public-access-block",
                            "Enable S3 Block Public Access",
                            "Apply S3 Block Public Access configuration at bucket level to prevent accidental public data leakage.",
                            ActionSafety.REQUIRES_APPROVAL,
                            List.of(
                                    "Review S3 bucket policies for intended public access",
                                    "Enable BlockPublicAcls, IgnorePublicAcls, BlockPublicPolicy, RestrictPublicBuckets"
                            ),
                            "Verify S3 bucket public access block is active"
                    );

                    riskMap.put(riskId, new OperationalRisk(
                            riskId,
                            RiskCategory.SECURITY,
                            RiskSeverity.HIGH,
                            "S3 bucket public access control gap",
                            "S3 bucket lacks complete S3 Block Public Access configuration.",
                            "Risk of unintentional data leakage or unauthorized public object reads.",
                            List.of("SecS3PublicAccessBlockRule"),
                            evidence,
                            detectedAt,
                            action,
                            RiskSource.COMPLIANCE
                    ));
                }
            }
        }

        // 3. Evaluate Security Topology Signals (R008)
        if (securityExposures != null) {
            for (SecurityExposureResult exp : securityExposures) {
                if (exp != null && exp.status() == ExposureStatus.EXPOSED) {
                    String riskId = "risk-security-exposure-" + exp.resourceId();
                    if (!riskMap.containsKey(riskId)) {
                        Map<String, Object> evidence = new LinkedHashMap<>();
                        evidence.put("resourceId", exp.resourceId());
                        evidence.put("resourceType", exp.resourceType());
                        evidence.put("exposureEvidence", exp.exposureEvidence());

                        RecommendedAction action = new RecommendedAction(
                                "isolate-exposed-resource-" + exp.resourceId(),
                                "Review network isolation for " + exp.resourceId(),
                                "Verify whether direct internet reachability is required for this resource and apply restrictive security group rules.",
                                ActionSafety.REQUIRES_APPROVAL,
                                List.of(
                                        "Audit ingress path for resource " + exp.resourceId(),
                                        "Restrict access to authorized VPC or ALB ingress"
                                ),
                                "Verify exposure status is NOT_EXPOSED on /api/v1/security/exposures"
                        );

                        riskMap.put(riskId, new OperationalRisk(
                                riskId,
                                RiskCategory.SECURITY,
                                RiskSeverity.HIGH,
                                "Direct internet exposure on " + exp.resourceType() + " (" + exp.resourceId() + ")",
                                "Topology reachability analysis identified direct public internet reachability to " + exp.resourceId(),
                                "Resource is directly reachable from external networks, increasing exposure to attacks.",
                                List.of(exp.resourceId()),
                                evidence,
                                detectedAt,
                                action,
                                RiskSource.SECURITY
                        ));
                    }
                }
            }
        }

        // 4. Evaluate Drift Signals (R009, R010)
        if (driftResults != null) {
            for (DriftResourceResult drift : driftResults) {
                if (drift == null) continue;

                // R009: Out-of-Band Security Group Drift
                if (drift.status() == DriftStatus.DRIFTED) {
                    boolean isSecurityGroup = (drift.resourceType() != null && drift.resourceType().toLowerCase().contains("security_group"))
                            || (drift.resourceAddress() != null && drift.resourceAddress().toLowerCase().contains("security_group"));

                    if (isSecurityGroup) {
                        String stableKey = (drift.resourceId() != null && !drift.resourceId().isBlank())
                                ? drift.resourceId()
                                : drift.resourceAddress();
                        String riskId = "risk-drift-security-" + stableKey;

                        Map<String, Object> evidence = new LinkedHashMap<>();
                        evidence.put("resourceAddress", drift.resourceAddress());
                        evidence.put("resourceId", drift.resourceId());
                        evidence.put("driftStatus", drift.status().name());
                        evidence.put("differenceCount", drift.differences() != null ? drift.differences().size() : 0);
                        evidence.put("explanation", drift.explanation());

                        RecommendedAction action = new RecommendedAction(
                                "reconcile-security-group-drift-" + stableKey,
                                "Reconcile security group with approved infrastructure baseline",
                                "Review out-of-band rules on security group " + stableKey + " and align live state with version-controlled Terraform configuration.",
                                ActionSafety.REQUIRES_APPROVAL,
                                List.of(
                                        "Inspect differences in drift evaluation report for " + drift.resourceAddress(),
                                        "Revert unapproved manual security group rules or update Terraform source code",
                                        "Execute terraform plan to confirm zero remaining drift"
                                ),
                                "Re-evaluate drift on /api/v1/drift/evaluate to confirm IN_SYNC state"
                        );

                        riskMap.put(riskId, new OperationalRisk(
                                riskId,
                                RiskCategory.SECURITY,
                                RiskSeverity.HIGH,
                                "Security group configuration drift: " + stableKey,
                                "Security group " + stableKey + " has drifted from its approved Terraform baseline with " + (drift.differences() != null ? drift.differences().size() : 0) + " attribute difference(s).",
                                "Out-of-band network rule modifications bypass peer-reviewed security policy and may introduce unauthorized access.",
                                List.of(stableKey),
                                evidence,
                                detectedAt,
                                action,
                                RiskSource.DRIFT
                        ));
                    }
                }
                // R010: Missing IaC Resource (NOT_FOUND)
                else if (drift.status() == DriftStatus.NOT_FOUND) {
                    String stableKey = drift.resourceAddress();
                    String riskId = "risk-drift-missing-" + stableKey;

                    Map<String, Object> evidence = new LinkedHashMap<>();
                    evidence.put("resourceAddress", drift.resourceAddress());
                    evidence.put("resourceId", drift.resourceId());
                    evidence.put("resourceType", drift.resourceType());
                    evidence.put("driftStatus", drift.status().name());
                    evidence.put("explanation", drift.explanation());

                    RecommendedAction action = new RecommendedAction(
                            "reprovision-missing-resource-" + stableKey,
                            "Reconcile missing production resource through approved pipeline",
                            "Investigate why declared resource " + stableKey + " is absent from AWS and re-provision via automated CI/CD pipeline.",
                            ActionSafety.HIGH_RISK,
                            List.of(
                                    "Verify whether resource " + stableKey + " was intentionally decommissioned",
                                    "If required, trigger Terraform apply via approved release pipeline to restore resource",
                                    "If deprecated, remove resource declaration from Terraform codebase"
                            ),
                            "Verify resource presence in AWS Discovery and confirm drift status is IN_SYNC"
                    );

                    riskMap.put(riskId, new OperationalRisk(
                            riskId,
                            RiskCategory.OPERATIONAL,
                            RiskSeverity.CRITICAL,
                            "Missing IaC-managed infrastructure resource: " + stableKey,
                            "Declared Terraform resource " + stableKey + " was not found in live AWS environment.",
                            "Production architecture is incomplete or damaged, which may cause immediate service failure or deployment pipeline blockage.",
                            List.of(stableKey),
                            evidence,
                            detectedAt,
                            action,
                            RiskSource.DRIFT
                    ));
                }
            }
        }

        // 5. Evaluate Resilience Incidents (R011)
        if (activeIncidents != null) {
            for (IncidentRecord inc : activeIncidents) {
                if (inc == null) continue;

                boolean isActive = (inc.status() == IncidentStatus.OPEN
                        || inc.status() == IncidentStatus.DEGRADED
                        || inc.status() == IncidentStatus.ACKNOWLEDGED);

                if (isActive && inc.severity() == IncidentSeverity.CRITICAL && CONTROL_PLANE_INCIDENT_TYPES.contains(inc.type())) {
                    String riskId = "risk-resilience-incident-" + inc.incidentId();

                    Map<String, Object> evidence = new LinkedHashMap<>();
                    evidence.put("incidentId", inc.incidentId());
                    evidence.put("incidentType", inc.type().name());
                    evidence.put("occurrenceCount", inc.occurrenceCount());
                    evidence.put("lastObservedAt", inc.lastObservedAt());
                    evidence.put("summary", inc.message());

                    RecommendedAction action = new RecommendedAction(
                            "investigate-control-plane-incident-" + inc.incidentId(),
                            "Investigate active control-plane incident and verify AWS service health",
                            "Address active critical incident " + inc.type().name() + " affecting control-plane telemetry and discovery.",
                            ActionSafety.READ_ONLY,
                            List.of(
                                    "Check AWS Health Dashboard and API rate limits in " + region,
                                    "Inspect incident details on /operations/incidents",
                                    "Verify AWS credentials, IAM permissions, or network connectivity"
                            ),
                            "Confirm incident status transitions to RESOLVED on /operations/incidents"
                    );

                    riskMap.put(riskId, new OperationalRisk(
                            riskId,
                            RiskCategory.OPERATIONAL,
                            RiskSeverity.CRITICAL,
                            "Active critical control-plane incident: " + inc.type().name(),
                            "Active critical incident (" + inc.type().name() + ") with " + inc.occurrenceCount() + " occurrence(s) detected.",
                            "Control plane observation or discovery is degraded, creating an operational blindspot for production operations.",
                            List.of(inc.incidentId()),
                            evidence,
                            detectedAt,
                            action,
                            RiskSource.RESILIENCE
                    ));
                }
            }
        }

        // 6. Evaluate Evidence Freshness (R012)
        if (evidenceStates != null) {
            for (EvidenceLifecycleRecord ev : evidenceStates) {
                if (ev == null) continue;

                if (ev.freshnessState() == EvidenceFreshnessState.EXPIRED) {
                    String subsystem = ev.evidenceType();
                    String riskId = "risk-resilience-evidence-" + subsystem;

                    Map<String, Object> evidence = new LinkedHashMap<>();
                    evidence.put("subsystem", subsystem);
                    evidence.put("ageSeconds", ev.ageSeconds());
                    evidence.put("freshnessState", ev.freshnessState().name());
                    evidence.put("lastSuccessfulSync", ev.lastSuccessfulSync());

                    RecommendedAction action = new RecommendedAction(
                            "refresh-expired-evidence-" + subsystem.toLowerCase(),
                            "Refresh analytical evidence for " + subsystem,
                            "Trigger background or manual analytical sync to refresh expired operational evidence for " + subsystem + ".",
                            ActionSafety.READ_ONLY,
                            List.of(
                                    "Trigger analytical snapshot refresh on /api/v1/dashboard/snapshot/refresh",
                                    "Verify background sync worker health and AWS API connectivity"
                            ),
                            "Verify evidence freshness state improves to FRESH on /operations/evidence"
                    );

                    riskMap.put(riskId, new OperationalRisk(
                            riskId,
                            RiskCategory.OPERATIONAL,
                            RiskSeverity.MEDIUM,
                            "Expired analytical evidence blindspot: " + subsystem,
                            "Evidence lifecycle record for " + subsystem + " has expired (age: " + ev.ageSeconds() + " seconds).",
                            "Operational and compliance evaluations are operating on stale data, potentially obscuring recent production changes.",
                            List.of(subsystem),
                            evidence,
                            detectedAt,
                            action,
                            RiskSource.RESILIENCE
                    ));
                }
            }
        }

        log.info("Correlated {} unique operational risks for account: {}, region: {}", riskMap.size(), accountId, region);
        return new ArrayList<>(riskMap.values());
    }
}
