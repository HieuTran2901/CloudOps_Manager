package com.cloudops.manager.aws.federation.service;

import com.cloudops.manager.aws.federation.model.*;
import com.cloudops.manager.aws.sts.model.AssumeRoleRequest;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.common.exception.AwsAccessDeniedException;
import com.cloudops.manager.common.exception.AwsThrottlingException;
import com.cloudops.manager.common.exception.AwsTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AwsFederationService {

    private static final Logger log = LoggerFactory.getLogger(AwsFederationService.class);

    private final AwsIdentityService identityService;
    private final String defaultRegion;

    private final AtomicReference<AwsAccountContext> currentContext = new AtomicReference<>();
    private final Map<String, AwsAccountContext> configuredAccounts = new ConcurrentHashMap<>();

    public AwsFederationService(
            AwsIdentityService identityService,
            @Value("${cloudops.aws.region:us-east-1}") String defaultRegion) {
        this.identityService = identityService;
        this.defaultRegion = defaultRegion;

        // Initialize with default local caller context
        try {
            var identity = identityService.getCurrentIdentity();
            AwsAccountContext defaultCtx = new AwsAccountContext(
                    identity.accountId(),
                    "Default Account",
                    defaultRegion,
                    null,
                    true,
                    FederationStatus.FEDERATED
            );
            currentContext.set(defaultCtx);
            configuredAccounts.put(identity.accountId(), defaultCtx);
        } catch (Exception e) {
            AwsAccountContext fallbackCtx = new AwsAccountContext(
                    "LOCAL_ACCOUNT",
                    "Local Development Account",
                    defaultRegion,
                    null,
                    true,
                    FederationStatus.UNKNOWN
            );
            currentContext.set(fallbackCtx);
            configuredAccounts.put("LOCAL_ACCOUNT", fallbackCtx);
        }
    }

    public FederationResult federateAccount(FederationRequest request) {
        if (request == null || request.targetAccountId() == null || request.roleArn() == null) {
            return new FederationResult(
                    FederationStatus.INVALID_ROLE,
                    request != null ? request.targetAccountId() : "UNKNOWN",
                    null,
                    null,
                    request != null && request.region() != null ? request.region() : defaultRegion,
                    "Target account ID and Role ARN must not be null.",
                    Instant.now()
            );
        }

        String region = (request.region() != null && !request.region().isBlank()) ? request.region() : defaultRegion;
        String sessionName = (request.roleSessionName() != null && !request.roleSessionName().isBlank())
                ? request.roleSessionName()
                : "cloudops-fed-" + System.currentTimeMillis();

        // 1. Validate Target Role & Account Matching
        AwsRoleTarget roleTarget;
        try {
            roleTarget = new AwsRoleTarget(
                    request.roleArn(),
                    request.targetAccountId(),
                    sessionName,
                    request.externalId(),
                    region
            );
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role federation target parameters: {}", e.getMessage());
            return new FederationResult(
                    FederationStatus.INVALID_ROLE,
                    request.targetAccountId(),
                    request.roleArn(),
                    sessionName,
                    region,
                    e.getMessage(),
                    Instant.now()
            );
        }

        // 2. Perform STS AssumeRole
        try {
            AssumeRoleRequest assumeReq = new AssumeRoleRequest(
                    roleTarget.roleArn(),
                    roleTarget.roleSessionName(),
                    roleTarget.externalId(),
                    3600
            );
            AssumedRoleSession session = identityService.assumeRole(assumeReq);

            // 3. Validate returned role user ARN matches expected target account
            if (session.assumedRoleUserArn() != null && !session.assumedRoleUserArn().contains(":" + roleTarget.targetAccountId() + ":")) {
                log.error("Assumed role session account mismatch for target: {}", roleTarget.targetAccountId());
                return new FederationResult(
                        FederationStatus.ACCOUNT_MISMATCH,
                        roleTarget.targetAccountId(),
                        roleTarget.roleArn(),
                        roleTarget.roleSessionName(),
                        region,
                        "Assumed role identity does not match expected target account ID.",
                        Instant.now()
                );
            }

            // 4. Update Active Account Context
            AwsAccountContext newCtx = new AwsAccountContext(
                    roleTarget.targetAccountId(),
                    "Federated Account (" + roleTarget.targetAccountId() + ")",
                    region,
                    roleTarget.roleArn(),
                    true,
                    FederationStatus.FEDERATED
            );
            currentContext.set(newCtx);
            configuredAccounts.put(roleTarget.targetAccountId(), newCtx);

            log.info("Successfully federated into AWS account: {} via role: {}", roleTarget.targetAccountId(), roleTarget.roleArn());

            return new FederationResult(
                    FederationStatus.FEDERATED,
                    roleTarget.targetAccountId(),
                    roleTarget.roleArn(),
                    roleTarget.roleSessionName(),
                    region,
                    "Account federation successful.",
                    Instant.now()
            );

        } catch (AwsAccessDeniedException e) {
            log.warn("STS AssumeRole access denied for role: {}", roleTarget.roleArn());
            return new FederationResult(
                    FederationStatus.ACCESS_DENIED,
                    roleTarget.targetAccountId(),
                    roleTarget.roleArn(),
                    roleTarget.roleSessionName(),
                    region,
                    "Access denied when assuming role in target account.",
                    Instant.now()
            );
        } catch (AwsThrottlingException e) {
            log.warn("STS AssumeRole rate exceeded for role: {}", roleTarget.roleArn());
            return new FederationResult(
                    FederationStatus.AWS_THROTTLED,
                    roleTarget.targetAccountId(),
                    roleTarget.roleArn(),
                    roleTarget.roleSessionName(),
                    region,
                    "STS request rate limit exceeded.",
                    Instant.now()
            );
        } catch (AwsTimeoutException e) {
            log.warn("STS AssumeRole timed out for role: {}", roleTarget.roleArn());
            return new FederationResult(
                    FederationStatus.AWS_TIMEOUT,
                    roleTarget.targetAccountId(),
                    roleTarget.roleArn(),
                    roleTarget.roleSessionName(),
                    region,
                    "STS operation timed out.",
                    Instant.now()
            );
        } catch (Exception e) {
            log.error("Unexpected failure during account federation: {}", e.getMessage());
            return new FederationResult(
                    FederationStatus.AWS_UNAVAILABLE,
                    roleTarget.targetAccountId(),
                    roleTarget.roleArn(),
                    roleTarget.roleSessionName(),
                    region,
                    "AWS STS service is currently unavailable.",
                    Instant.now()
            );
        }
    }

    public AwsAccountContext getCurrentContext() {
        return currentContext.get();
    }

    public List<AwsAccountContext> listConfiguredAccounts() {
        return new ArrayList<>(configuredAccounts.values());
    }
}