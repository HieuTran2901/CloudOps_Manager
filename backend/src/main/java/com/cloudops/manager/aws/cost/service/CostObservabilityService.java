package com.cloudops.manager.aws.cost.service;

import com.cloudops.manager.aws.cost.model.CostAggregationResult;
import com.cloudops.manager.aws.cost.model.CostQueryRequest;
import com.cloudops.manager.aws.cost.provider.CostExplorerProvider;
import com.cloudops.manager.aws.discovery.config.AwsClientFactory;
import com.cloudops.manager.aws.sts.model.AssumeRoleRequest;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class CostObservabilityService {

    private static final Logger log = LoggerFactory.getLogger(CostObservabilityService.class);

    private final CostExplorerProvider costExplorerProvider;
    private final AwsIdentityService awsIdentityService;
    private final AwsClientFactory awsClientFactory;

    public CostObservabilityService(
            CostExplorerProvider costExplorerProvider,
            AwsIdentityService awsIdentityService,
            AwsClientFactory awsClientFactory) {
        this.costExplorerProvider = costExplorerProvider;
        this.awsIdentityService = awsIdentityService;
        this.awsClientFactory = awsClientFactory;
    }

    public CostAggregationResult getCostAndUsage(
            String metric,
            String granularity,
            LocalDate startDate,
            LocalDate endDate,
            List<String> groupByDimensions,
            Map<String, List<String>> filters) {

        String accountId = awsIdentityService.getCurrentIdentity().accountId();
        CostQueryRequest request = buildValidatedRequest(accountId, metric, granularity, startDate, endDate, groupByDimensions, filters);

        return costExplorerProvider.getCostAndUsage(request, null, "STANDALONE_ACCOUNT");
    }

    public CostAggregationResult getCrossAccountCostAndUsage(
            AwsAccountTarget target,
            String metric,
            String granularity,
            LocalDate startDate,
            LocalDate endDate,
            List<String> groupByDimensions,
            Map<String, List<String>> filters) {

        log.info("Initiating cross-account Cost Explorer query for account: {}, role: {}", target.accountId(), target.roleArn());
        AssumedRoleSession session = awsIdentityService.assumeRole(
                new AssumeRoleRequest(target.roleArn(), target.roleSessionName(), target.externalId(), 900)
        );

        try (StsClient sts = awsClientFactory.createStsClient(session, "us-east-1");
             CostExplorerClient ce = awsClientFactory.createCostExplorerClient(session)) {

            String verifiedAccount = sts.getCallerIdentity(GetCallerIdentityRequest.builder().build()).account();
            if (!target.accountId().equals(verifiedAccount)) {
                throw new IllegalStateException("Assumed caller identity account " + verifiedAccount + " does not match target account " + target.accountId());
            }

            CostQueryRequest request = buildValidatedRequest(target.accountId(), metric, granularity, startDate, endDate, groupByDimensions, filters);
            return costExplorerProvider.getCostAndUsage(request, ce, "CROSS_ACCOUNT_ASSUMED");
        }
    }

    private CostQueryRequest buildValidatedRequest(
            String accountId,
            String metric,
            String granularity,
            LocalDate startDate,
            LocalDate endDate,
            List<String> groupByDimensions,
            Map<String, List<String>> filters) {

        String resolvedMetric = (metric != null && !metric.isBlank()) ? metric.trim() : "UnblendedCost";
        String resolvedGranularity = (granularity != null && !granularity.isBlank()) ? granularity.trim().toUpperCase() : "MONTHLY";
        LocalDate resolvedEnd = endDate != null ? endDate : LocalDate.now();
        LocalDate resolvedStart = startDate != null ? startDate : ("DAILY".equalsIgnoreCase(resolvedGranularity) ? resolvedEnd.minusDays(30) : resolvedEnd.minusMonths(3).withDayOfMonth(1));

        CostQueryRequest req = new CostQueryRequest(accountId, resolvedMetric, resolvedGranularity, resolvedStart, resolvedEnd, groupByDimensions, filters);
        CostValidationUtils.validateRequest(req);
        return req;
    }
}