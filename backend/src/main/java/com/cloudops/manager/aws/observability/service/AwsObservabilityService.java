package com.cloudops.manager.aws.observability.service;

import com.cloudops.manager.aws.discovery.config.AwsClientFactory;
import com.cloudops.manager.aws.observability.model.*;
import com.cloudops.manager.aws.observability.provider.CloudWatchMetricsProvider;
import com.cloudops.manager.aws.sts.model.AssumeRoleRequest;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class AwsObservabilityService {

    private static final Logger log = LoggerFactory.getLogger(AwsObservabilityService.class);
    private static final Set<String> ALLOWED_STATS = Set.of("Average", "Sum", "SampleCount", "Maximum", "Minimum");
    private static final Duration MAX_WINDOW = Duration.ofDays(30);

    private final CloudWatchMetricsProvider metricsProvider;
    private final AwsIdentityService awsIdentityService;
    private final AwsClientFactory awsClientFactory;

    @Value("${cloudops.aws.region:us-east-1}")
    private String defaultRegion;

    public AwsObservabilityService(
            CloudWatchMetricsProvider metricsProvider,
            AwsIdentityService awsIdentityService,
            AwsClientFactory awsClientFactory) {
        this.metricsProvider = metricsProvider;
        this.awsIdentityService = awsIdentityService;
        this.awsClientFactory = awsClientFactory;
    }

    public MetricSeries getEc2Metric(String instanceId, String metricName, Instant startTime, Instant endTime, Integer period, String stat, String region) {
        MetricQuery query = buildValidatedQuery(instanceId, metricName, startTime, endTime, period, stat, region);
        return metricsProvider.getEc2Metric(query);
    }

    public MetricSeries getRdsMetric(String dbId, String metricName, Instant startTime, Instant endTime, Integer period, String stat, String region) {
        MetricQuery query = buildValidatedQuery(dbId, metricName, startTime, endTime, period, stat, region);
        return metricsProvider.getRdsMetric(query);
    }

    public TelemetryAggregationResult getAggregatedMetrics(
            String resourceType, List<String> resourceIds, List<String> metricNames,
            Instant startTime, Instant endTime, Integer periodSeconds, String statistic,
            Integer downsampleFactor, String rollupStatistic, String optionalRegion) {
        String region = resolveEffectiveRegion(optionalRegion);
        String accountId = awsIdentityService.getCurrentIdentity().accountId();
        return executeQuery(resourceType, resourceIds, metricNames, startTime, endTime, periodSeconds, statistic,
                downsampleFactor, rollupStatistic, region, accountId, null);
    }

    public TelemetryAggregationResult getCrossAccountMetrics(
            AwsAccountTarget target, String resourceType, List<String> resourceIds, List<String> metricNames,
            Instant startTime, Instant endTime, Integer periodSeconds, String statistic,
            Integer downsampleFactor, String rollupStatistic) {
        String region = resolveEffectiveRegion(target.region());
        AssumedRoleSession session = awsIdentityService.assumeRole(
                new AssumeRoleRequest(target.roleArn(), target.roleSessionName(), target.externalId(), 900)
        );

        try (StsClient sts = awsClientFactory.createStsClient(session, region);
             CloudWatchClient cw = awsClientFactory.createCloudWatchClient(session, region)) {

            String verified = sts.getCallerIdentity(GetCallerIdentityRequest.builder().build()).account();
            if (!target.accountId().equals(verified)) {
                throw new IllegalStateException("Assumed caller identity account " + verified + " does not match target account " + target.accountId());
            }

            return executeQuery(resourceType, resourceIds, metricNames, startTime, endTime, periodSeconds, statistic,
                    downsampleFactor, rollupStatistic, region, target.accountId(), cw);
        }
    }

    private TelemetryAggregationResult executeQuery(
            String resourceType, List<String> resourceIds, List<String> metricNames,
            Instant startTime, Instant endTime, Integer periodSeconds, String statistic,
            Integer downsampleFactor, String rollupStatistic, String region, String accountId,
            CloudWatchClient targetClient) {

        validateBatchInputs(resourceType, resourceIds, metricNames);
        Instant end = endTime != null ? endTime : Instant.now();
        Instant start = startTime != null ? startTime : end.minus(Duration.ofHours(1));
        validateTimeWindow(start, end);

        int period = periodSeconds != null ? periodSeconds : 300;
        validatePeriod(period);

        String stat = (statistic != null && !statistic.isBlank()) ? statistic.trim() : "Average";
        if (!ALLOWED_STATS.contains(stat)) {
            throw new IllegalArgumentException("Invalid statistic '" + stat + "'. Allowed: " + ALLOWED_STATS);
        }

        String namespace = resolveNamespace(resourceType);
        String dimName = resolveDimensionName(resourceType);

        List<MetricDataQueryRequest> requests = new ArrayList<>();
        for (String resId : resourceIds) {
            for (String mName : metricNames) {
                requests.add(new MetricDataQueryRequest(
                        namespace, mName.trim(), Map.of(dimName, resId.trim()), stat, period,
                        start, end, resId.trim(), resourceType.toUpperCase(), accountId, region,
                        downsampleFactor, rollupStatistic
                ));
            }
        }

        List<MetricSeries> series = metricsProvider.queryMetricData(requests, targetClient);
        int totalDatapoints = series.stream().mapToInt(s -> s.dataPoints().size()).sum();

        return new TelemetryAggregationResult(accountId, region, start, end, series.size(), totalDatapoints, series, Instant.now());
    }

    private MetricQuery buildValidatedQuery(String resId, String metricName, Instant start, Instant end, Integer period, String stat, String region) {
        if (resId == null || resId.isBlank()) throw new IllegalArgumentException("resourceId must not be null or blank");
        if (metricName == null || metricName.isBlank()) throw new IllegalArgumentException("metricName must not be null or blank");
        Instant resolvedEnd = end != null ? end : Instant.now();
        Instant resolvedStart = start != null ? start : resolvedEnd.minus(Duration.ofHours(1));
        validateTimeWindow(resolvedStart, resolvedEnd);
        int p = period != null ? period : 300;
        validatePeriod(p);
        String s = (stat != null && !stat.isBlank()) ? stat.trim() : "Average";
        if (!ALLOWED_STATS.contains(s)) throw new IllegalArgumentException("Invalid statistic: " + s);
        return new MetricQuery(metricName.trim(), resId.trim(), resolveEffectiveRegion(region), awsIdentityService.getCurrentIdentity().accountId(), resolvedStart, resolvedEnd, p, s);
    }

    private String resolveEffectiveRegion(String region) {
        return (region != null && !region.isBlank()) ? region.trim() : defaultRegion;
    }

    private String resolveNamespace(String type) {
        return switch (type.toUpperCase()) {
            case "EC2" -> "AWS/EC2";
            case "RDS" -> "AWS/RDS";
            case "ENI", "NETWORK_INTERFACE" -> "AWS/EC2";
            default -> throw new IllegalArgumentException("Unsupported telemetry resource type: " + type);
        };
    }

    private String resolveDimensionName(String type) {
        return switch (type.toUpperCase()) {
            case "EC2" -> "InstanceId";
            case "RDS" -> "DBInstanceIdentifier";
            case "ENI", "NETWORK_INTERFACE" -> "NetworkInterfaceId";
            default -> throw new IllegalArgumentException("Unsupported telemetry resource type: " + type);
        };
    }

    private void validateBatchInputs(String type, List<String> resourceIds, List<String> metricNames) {
        if (type == null || type.isBlank()) throw new IllegalArgumentException("resourceType must not be null or blank");
        if (resourceIds == null || resourceIds.isEmpty() || resourceIds.size() > 50) {
            throw new IllegalArgumentException("resourceIds must contain between 1 and 50 identifiers");
        }
        if (metricNames == null || metricNames.isEmpty() || metricNames.size() > 20) {
            throw new IllegalArgumentException("metricNames must contain between 1 and 20 metric names");
        }
    }

    private void validateTimeWindow(Instant start, Instant end) {
        if (!start.isBefore(end)) throw new IllegalArgumentException("startTime must be strictly before endTime");
        if (Duration.between(start, end).compareTo(MAX_WINDOW) > 0) throw new IllegalArgumentException("Query window cannot exceed 30 days");
    }

    private void validatePeriod(int p) {
        if (p < 60 || p > 86400 || p % 60 != 0) {
            throw new IllegalArgumentException("period must be a positive multiple of 60 seconds (between 60 and 86400)");
        }
    }
}