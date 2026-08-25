package com.cloudops.manager.aws.audit.service;

import com.cloudops.manager.aws.audit.model.CloudTrailEventLookupRequest;
import com.cloudops.manager.aws.audit.model.CloudTrailEventResult;
import com.cloudops.manager.aws.audit.provider.CloudTrailProvider;
import com.cloudops.manager.aws.discovery.config.AwsClientFactory;
import com.cloudops.manager.aws.sts.model.AssumeRoleRequest;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

import java.time.Duration;
import java.time.Instant;

@Service
public class CloudTrailAuditService {

    private static final Logger log = LoggerFactory.getLogger(CloudTrailAuditService.class);

    private final CloudTrailProvider cloudTrailProvider;
    private final AwsIdentityService awsIdentityService;
    private final AwsClientFactory awsClientFactory;

    @Value("${cloudops.aws.region:us-east-1}")
    private String defaultRegion;

    public CloudTrailAuditService(
            CloudTrailProvider cloudTrailProvider,
            AwsIdentityService awsIdentityService,
            AwsClientFactory awsClientFactory) {
        this.cloudTrailProvider = cloudTrailProvider;
        this.awsIdentityService = awsIdentityService;
        this.awsClientFactory = awsClientFactory;
    }

    public CloudTrailEventResult lookupEvents(
            String eventName,
            String username,
            String resourceName,
            String resourceType,
            Instant startTime,
            Instant endTime,
            Integer maxResults,
            String optionalRegion) {

        String region = resolveEffectiveRegion(optionalRegion);
        String accountId = awsIdentityService.getCurrentIdentity().accountId();
        CloudTrailEventLookupRequest request = buildValidatedRequest(accountId, region, eventName, username, resourceName, resourceType, startTime, endTime, maxResults);

        return cloudTrailProvider.lookupEvents(request, null);
    }

    public CloudTrailEventResult lookupCrossAccountEvents(
            AwsAccountTarget target,
            String eventName,
            String username,
            String resourceName,
            String resourceType,
            Instant startTime,
            Instant endTime,
            Integer maxResults) {

        String region = resolveEffectiveRegion(target.region());
        log.info("Initiating cross-account CloudTrail audit for account: {}, role: {}, region: {}", target.accountId(), target.roleArn(), region);

        AssumedRoleSession session = awsIdentityService.assumeRole(
                new AssumeRoleRequest(target.roleArn(), target.roleSessionName(), target.externalId(), 900)
        );

        try (StsClient sts = awsClientFactory.createStsClient(session, region);
             CloudTrailClient ct = awsClientFactory.createCloudTrailClient(session, region)) {

            String verifiedAccount = sts.getCallerIdentity(GetCallerIdentityRequest.builder().build()).account();
            if (!target.accountId().equals(verifiedAccount)) {
                throw new IllegalStateException("Assumed caller identity account " + verifiedAccount + " does not match target account " + target.accountId());
            }

            CloudTrailEventLookupRequest request = buildValidatedRequest(target.accountId(), region, eventName, username, resourceName, resourceType, startTime, endTime, maxResults);
            return cloudTrailProvider.lookupEvents(request, ct);
        }
    }

    private CloudTrailEventLookupRequest buildValidatedRequest(
            String accountId,
            String region,
            String eventName,
            String username,
            String resourceName,
            String resourceType,
            Instant startTime,
            Instant endTime,
            Integer maxResults) {

        Instant resolvedEnd = endTime != null ? endTime : Instant.now();
        Instant resolvedStart = startTime != null ? startTime : resolvedEnd.minus(Duration.ofDays(7));
        int resolvedLimit = (maxResults != null && maxResults > 0) ? maxResults : 50;

        CloudTrailEventLookupRequest request = new CloudTrailEventLookupRequest(
                accountId, region, eventName, username, resourceName, resourceType, resolvedStart, resolvedEnd, resolvedLimit
        );
        CloudTrailValidationUtils.validateRequest(request);
        return request;
    }

    private String resolveEffectiveRegion(String region) {
        return (region != null && !region.isBlank()) ? region.trim() : defaultRegion;
    }
}