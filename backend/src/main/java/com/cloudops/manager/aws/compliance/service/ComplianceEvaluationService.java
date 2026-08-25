package com.cloudops.manager.aws.compliance.service;

import com.cloudops.manager.aws.audit.model.CloudTrailEventResource;
import com.cloudops.manager.aws.audit.model.CloudTrailEventResult;
import com.cloudops.manager.aws.audit.service.CloudTrailAuditService;
import com.cloudops.manager.aws.compliance.model.*;
import com.cloudops.manager.aws.compliance.rules.ComplianceRuleRegistry;
import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class ComplianceEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceEvaluationService.class);

    private final ComplianceRuleRegistry ruleRegistry;
    private final AwsResourceDiscoveryService discoveryService;
    private final AwsIdentityService identityService;
    private final CloudTrailAuditService auditService;

    @Value("${cloudops.aws.region:us-east-1}")
    private String defaultRegion;

    public ComplianceEvaluationService(
            ComplianceRuleRegistry ruleRegistry,
            AwsResourceDiscoveryService discoveryService,
            AwsIdentityService identityService,
            CloudTrailAuditService auditService) {
        this.ruleRegistry = ruleRegistry;
        this.discoveryService = discoveryService;
        this.identityService = identityService;
        this.auditService = auditService;
    }

    public List<ComplianceRule> getRegisteredRules() {
        return ruleRegistry.getAllRules();
    }

    public ComplianceEvaluationReport evaluateLocal(String optionalRegion, List<String> ruleFilter) {
        String region = (optionalRegion != null && !optionalRegion.isBlank()) ? optionalRegion.trim() : defaultRegion;
        String accountId = identityService.getCurrentIdentity().accountId();

        log.info("Evaluating compliance rules locally for account: {}, region: {}", accountId, region);

        InventorySummary summary = discoveryService.discoverAll(region);
        List<CloudResource> allResources = summary.resources();
        List<IamUserDetailResource> iamUsers = tryDiscoverIamUsers();
        List<SecurityGroupDetailResource> sgs = tryGetSecurityGroupDetails(summary.resources(), region);
        List<S3DetailResource> s3Buckets = tryGetS3BucketDetails(summary.resources(), region);
        List<RdsDetailResource> rdsDbs = tryGetRdsInstanceDetails(summary.resources(), region);
        List<Ec2DetailResource> ec2Instances = tryGetEc2InstanceDetails(summary.resources(), region);
        List<CloudTrailEventResource> auditEvents = tryGetRecentAuditEvents(region);

        Map<String, EvidenceAvailability> availability = new HashMap<>();
        availability.put("IAM", iamUsers != null ? EvidenceAvailability.COMPLETE : EvidenceAvailability.UNAVAILABLE);
        availability.put("SECURITY_GROUP", sgs != null ? EvidenceAvailability.COMPLETE : EvidenceAvailability.UNAVAILABLE);
        availability.put("S3", s3Buckets != null ? EvidenceAvailability.COMPLETE : EvidenceAvailability.UNAVAILABLE);
        availability.put("RDS", rdsDbs != null ? EvidenceAvailability.COMPLETE : EvidenceAvailability.UNAVAILABLE);
        availability.put("EC2", ec2Instances != null ? EvidenceAvailability.COMPLETE : EvidenceAvailability.UNAVAILABLE);
        availability.put("CLOUDTRAIL", auditEvents != null ? EvidenceAvailability.COMPLETE : EvidenceAvailability.UNAVAILABLE);

        CorrelatedEvidenceSet evidenceSet = buildCorrelatedEvidenceSet(accountId, region, ec2Instances, sgs, iamUsers, s3Buckets, rdsDbs);

        ComplianceEvaluationContext context = new ComplianceEvaluationContext(
                accountId, region, allResources, iamUsers, sgs, s3Buckets, rdsDbs, ec2Instances, auditEvents, evidenceSet, availability, Map.of()
        );

        return executeRuleEvaluation(context, ruleFilter);
    }

    public ComplianceEvaluationReport evaluateCrossAccount(AwsAccountTarget target, List<String> ruleFilter) {
        String region = (target.region() != null && !target.region().isBlank()) ? target.region().trim() : defaultRegion;
        log.info("Evaluating compliance rules cross-account for target: {}, region: {}", target.accountId(), region);

        InventorySummary summary = discoveryService.discoverAccount(target);
        List<CloudResource> allResources = summary.resources();

        ComplianceEvaluationContext context = new ComplianceEvaluationContext(
                target.accountId(), region, allResources, List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), CorrelatedEvidenceSet.empty(), Map.of(), Map.of()
        );

        return executeRuleEvaluation(context, ruleFilter);
    }

    private ComplianceEvaluationReport executeRuleEvaluation(ComplianceEvaluationContext context, List<String> ruleFilter) {
        List<ComplianceRule> rulesToEvaluate = new ArrayList<>();
        if (ruleFilter != null && !ruleFilter.isEmpty()) {
            for (String rId : ruleFilter) {
                ruleRegistry.getRule(rId.trim()).ifPresent(rulesToEvaluate::add);
            }
        } else {
            rulesToEvaluate.addAll(ruleRegistry.getAllRules());
        }

        List<ComplianceEvaluationResult> results = new ArrayList<>();
        int passCount = 0;
        int failCount = 0;
        int naCount = 0;
        int insufficientCount = 0;

        for (ComplianceRule rule : rulesToEvaluate) {
            ComplianceEvaluationResult res = rule.evaluate(context);
            results.add(res);
            switch (res.status()) {
                case PASS -> passCount++;
                case FAIL -> failCount++;
                case NOT_APPLICABLE -> naCount++;
                case INSUFFICIENT_EVIDENCE -> insufficientCount++;
            }
        }

        return new ComplianceEvaluationReport(
                context.accountId(),
                context.region(),
                Instant.now(),
                results.size(),
                passCount,
                failCount,
                naCount,
                insufficientCount,
                results
        );
    }

    private CorrelatedEvidenceSet buildCorrelatedEvidenceSet(
            String accountId, String region,
            List<Ec2DetailResource> ec2List,
            List<SecurityGroupDetailResource> sgList,
            List<IamUserDetailResource> iamList,
            List<S3DetailResource> s3List,
            List<RdsDetailResource> rdsList) {

        List<CorrelatedEvidenceItem> items = new ArrayList<>();
        EvidenceScope scope = new EvidenceScope(accountId, region);

        if (ec2List != null) {
            for (Ec2DetailResource ec2 : ec2List) {
                items.add(new CorrelatedEvidenceItem("AWS::EC2::Instance", ec2.instanceId(), scope, "EC2_DISCOVERY",
                        Map.of("state", ec2.instanceState(), "publicIp", ec2.publicIp() != null ? ec2.publicIp() : "")));
            }
        }
        if (sgList != null) {
            for (SecurityGroupDetailResource sg : sgList) {
                items.add(new CorrelatedEvidenceItem("AWS::EC2::SecurityGroup", sg.securityGroupId(), scope, "VPC_DISCOVERY",
                        Map.of("name", sg.securityGroupName() != null ? sg.securityGroupName() : "")));
            }
        }
        return new CorrelatedEvidenceSet(items);
    }

    private List<IamUserDetailResource> tryDiscoverIamUsers() {
        try {
            List<IamUserResource> users = discoveryService.getIamUsers();
            if (users == null) return List.of();
            List<IamUserDetailResource> details = new ArrayList<>();
            for (IamUserResource u : users) {
                try {
                    details.add(discoveryService.getIamUserDetail(u.userName()));
                } catch (Exception ignored) {}
            }
            return details;
        } catch (Exception e) {
            log.warn("Unable to fetch IAM users: {}", e.getMessage());
            return null;
        }
    }

    private List<SecurityGroupDetailResource> tryGetSecurityGroupDetails(List<CloudResource> resources, String region) {
        if (resources == null) return List.of();
        List<SecurityGroupDetailResource> list = new ArrayList<>();
        for (CloudResource r : resources) {
            if (r instanceof SecurityGroupResource sgr) {
                try {
                    list.add(discoveryService.getSecurityGroupDetail(sgr.resourceId(), region));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    private List<S3DetailResource> tryGetS3BucketDetails(List<CloudResource> resources, String region) {
        if (resources == null) return List.of();
        List<S3DetailResource> list = new ArrayList<>();
        for (CloudResource r : resources) {
            if (r instanceof S3BucketResource br) {
                try {
                    list.add(discoveryService.getS3BucketDetail(br.name(), region));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    private List<RdsDetailResource> tryGetRdsInstanceDetails(List<CloudResource> resources, String region) {
        if (resources == null) return List.of();
        List<RdsDetailResource> list = new ArrayList<>();
        for (CloudResource r : resources) {
            if (r instanceof RdsInstanceResource rds) {
                try {
                    list.add(discoveryService.getRdsInstanceDetail(rds.resourceId(), region));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    private List<Ec2DetailResource> tryGetEc2InstanceDetails(List<CloudResource> resources, String region) {
        if (resources == null) return List.of();
        List<Ec2DetailResource> list = new ArrayList<>();
        for (CloudResource r : resources) {
            if (r instanceof Ec2InstanceResource ec2) {
                try {
                    list.add(discoveryService.getEc2InstanceDetail(ec2.resourceId(), region));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }

    private List<CloudTrailEventResource> tryGetRecentAuditEvents(String region) {
        try {
            CloudTrailEventResult result = auditService.lookupEvents(null, null, null, null, null, null, 50, region);
            return result != null ? result.events() : List.of();
        } catch (Exception e) {
            log.warn("Unable to fetch CloudTrail events: {}", e.getMessage());
            return null;
        }
    }
}