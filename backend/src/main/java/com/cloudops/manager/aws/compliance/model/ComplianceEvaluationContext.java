package com.cloudops.manager.aws.compliance.model;

import com.cloudops.manager.aws.audit.model.CloudTrailEventResource;
import com.cloudops.manager.aws.discovery.model.*;

import java.util.List;
import java.util.Map;

public record ComplianceEvaluationContext(
    String accountId,
    String region,
    List<CloudResource> discoveredResources,
    List<IamUserDetailResource> iamUsers,
    List<SecurityGroupDetailResource> securityGroups,
    List<S3DetailResource> s3Buckets,
    List<RdsDetailResource> rdsDatabases,
    List<Ec2DetailResource> ec2Instances,
    List<CloudTrailEventResource> recentAuditEvents,
    CorrelatedEvidenceSet correlatedEvidence,
    Map<String, EvidenceAvailability> availability,
    Map<String, Object> additionalEvidence
) {
    public ComplianceEvaluationContext(
            String accountId,
            String region,
            List<CloudResource> discoveredResources,
            List<IamUserDetailResource> iamUsers,
            List<SecurityGroupDetailResource> securityGroups,
            List<S3DetailResource> s3Buckets,
            List<RdsDetailResource> rdsDatabases,
            Map<String, Object> additionalEvidence) {
        this(accountId, region, discoveredResources, iamUsers, securityGroups, s3Buckets, rdsDatabases,
             List.of(), List.of(), CorrelatedEvidenceSet.empty(), Map.of(), additionalEvidence);
    }
}