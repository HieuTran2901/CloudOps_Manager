package com.cloudops.manager.aws.quota.service;

import com.cloudops.manager.aws.discovery.model.Ec2InstanceResource;
import com.cloudops.manager.aws.discovery.provider.Ec2Provider;
import com.cloudops.manager.aws.discovery.provider.RdsProvider;
import com.cloudops.manager.aws.discovery.provider.VpcProvider;
import com.cloudops.manager.aws.quota.model.QuotaStatus;
import com.cloudops.manager.aws.quota.model.QuotaUtilizationReport;
import com.cloudops.manager.aws.quota.model.ServiceQuotaItem;
import com.cloudops.manager.aws.quota.provider.ServiceQuotasProvider;
import com.cloudops.manager.aws.sts.provider.StsIdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServiceQuotasService {

    private static final Logger log = LoggerFactory.getLogger(ServiceQuotasService.class);

    private final ServiceQuotasProvider serviceQuotasProvider;
    private final Ec2Provider ec2Provider;
    private final VpcProvider vpcProvider;
    private final RdsProvider rdsProvider;
    private final StsIdentityProvider stsIdentityProvider;
    private final String defaultRegion;

    public ServiceQuotasService(
            ServiceQuotasProvider serviceQuotasProvider,
            Ec2Provider ec2Provider,
            VpcProvider vpcProvider,
            RdsProvider rdsProvider,
            StsIdentityProvider stsIdentityProvider,
            @Value("${cloudops.aws.region:ap-southeast-2}") String defaultRegion
    ) {
        this.serviceQuotasProvider = serviceQuotasProvider;
        this.ec2Provider = ec2Provider;
        this.vpcProvider = vpcProvider;
        this.rdsProvider = rdsProvider;
        this.stsIdentityProvider = stsIdentityProvider;
        this.defaultRegion = defaultRegion;
    }

    public QuotaUtilizationReport getQuotaUtilizationReport(String region) {
        String targetRegion = (region != null && !region.isBlank()) ? region : defaultRegion;
        String accountId = resolveAccountId();

        log.info("Generating quota utilization report for account: {}, region: {}", accountId, targetRegion);

        List<ServiceQuotaItem> rawQuotas = serviceQuotasProvider.listCoreServiceQuotas(targetRegion, accountId);
        
        // Correlate with discovered resource metrics
        Map<String, Double> discoveredMetrics = collectDiscoveredUsage(targetRegion, accountId);

        List<ServiceQuotaItem> correlatedQuotas = new ArrayList<>();
        int normalCount = 0;
        int warningCount = 0;
        int criticalCount = 0;
        int unknownCount = 0;
        double highestUtilization = 0.0;
        Map<String, Integer> statusSummary = new HashMap<>();
        statusSummary.put("NORMAL", 0);
        statusSummary.put("WARNING", 0);
        statusSummary.put("CRITICAL", 0);
        statusSummary.put("UNKNOWN", 0);

        for (ServiceQuotaItem raw : rawQuotas) {
            ServiceQuotaItem enriched = correlateAndEvaluate(raw, discoveredMetrics);
            correlatedQuotas.add(enriched);

            QuotaStatus status = enriched.status();
            statusSummary.put(status.name(), statusSummary.getOrDefault(status.name(), 0) + 1);

            switch (status) {
                case NORMAL -> normalCount++;
                case WARNING -> warningCount++;
                case CRITICAL -> criticalCount++;
                case UNKNOWN -> unknownCount++;
            }

            if (enriched.utilizationPercentage() != null && enriched.utilizationPercentage() > highestUtilization) {
                highestUtilization = enriched.utilizationPercentage();
            }
        }

        return new QuotaUtilizationReport(
                accountId,
                targetRegion,
                correlatedQuotas.size(),
                normalCount,
                warningCount,
                criticalCount,
                unknownCount,
                highestUtilization,
                correlatedQuotas,
                statusSummary,
                Instant.now()
        );
    }

    public List<ServiceQuotaItem> getQuotasForService(String serviceCode, String region) {
        String targetRegion = (region != null && !region.isBlank()) ? region : defaultRegion;
        String accountId = resolveAccountId();
        List<ServiceQuotaItem> rawQuotas = serviceQuotasProvider.listServiceQuotas(serviceCode, targetRegion, accountId);
        Map<String, Double> discoveredMetrics = collectDiscoveredUsage(targetRegion, accountId);

        return rawQuotas.stream()
                .map(raw -> correlateAndEvaluate(raw, discoveredMetrics))
                .toList();
    }

    public ServiceQuotaItem correlateAndEvaluate(ServiceQuotaItem raw, Map<String, Double> discoveredMetrics) {
        String quotaName = raw.quotaName() != null ? raw.quotaName() : "";
        String quotaCode = raw.quotaCode() != null ? raw.quotaCode() : "";
        String serviceCode = raw.serviceCode() != null ? raw.serviceCode().toLowerCase() : "";

        Double usage = null;
        String usageSource = "UNAVAILABLE";

        if ("vpc".equals(serviceCode) || quotaName.toLowerCase().contains("vpc")) {
            if (quotaName.toLowerCase().contains("vpcs per region") || "L-F678F13E".equalsIgnoreCase(quotaCode)) {
                usage = discoveredMetrics.get("vpc.count");
                usageSource = "VPC_DISCOVERY";
            }
        } else if ("ec2".equals(serviceCode) || quotaName.toLowerCase().contains("instance") || quotaName.toLowerCase().contains("vcpu")) {
            if (quotaName.toLowerCase().contains("standard") || "L-1216C47A".equalsIgnoreCase(quotaCode) || quotaName.toLowerCase().contains("instances")) {
                usage = discoveredMetrics.get("ec2.vcpus");
                usageSource = "EC2_VCPU_DISCOVERY";
            }
        } else if ("rds".equals(serviceCode) || quotaName.toLowerCase().contains("db instance")) {
            if (quotaName.toLowerCase().contains("db instances") || "L-7B6409FD".equalsIgnoreCase(quotaCode)) {
                usage = discoveredMetrics.get("rds.count");
                usageSource = "RDS_DISCOVERY";
            }
        }

        Double utilization = calculateUtilization(usage, raw.appliedLimit());
        QuotaStatus status = classifyStatus(utilization);

        return new ServiceQuotaItem(
                raw.serviceCode(),
                raw.serviceName(),
                raw.quotaCode(),
                raw.quotaName(),
                raw.appliedLimit(),
                usage,
                utilization,
                status,
                raw.region(),
                usageSource,
                raw.unit(),
                raw.adjustable(),
                Instant.now()
        );
    }

    public static Double calculateUtilization(Double currentUsage, Double appliedLimit) {
        if (currentUsage == null || appliedLimit == null || appliedLimit <= 0.0) {
            return null;
        }
        if (currentUsage < 0.0) {
            currentUsage = 0.0;
        }
        double rawPercentage = (currentUsage / appliedLimit) * 100.0;
        return BigDecimal.valueOf(rawPercentage)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static QuotaStatus classifyStatus(Double utilizationPercentage) {
        if (utilizationPercentage == null) {
            return QuotaStatus.UNKNOWN;
        }
        if (utilizationPercentage >= 90.0) {
            return QuotaStatus.CRITICAL;
        }
        if (utilizationPercentage >= 80.0) {
            return QuotaStatus.WARNING;
        }
        return QuotaStatus.NORMAL;
    }

    private Map<String, Double> collectDiscoveredUsage(String region, String accountId) {
        Map<String, Double> metrics = new HashMap<>();
        try {
            List<Ec2InstanceResource> instances = ec2Provider.describeInstances(region, accountId);
            double totalVcpus = Ec2VcpuResolver.sumTotalVcpus(instances);
            metrics.put("ec2.vcpus", totalVcpus);
            metrics.put("ec2.count", (double) instances.size());
        } catch (Exception e) {
            log.warn("Unable to collect EC2 discovered metrics: {}", e.getMessage());
            metrics.put("ec2.vcpus", 0.0);
            metrics.put("ec2.count", 0.0);
        }

        try {
            metrics.put("vpc.count", (double) vpcProvider.describeVpcs(region, accountId).size());
        } catch (Exception e) {
            log.warn("Unable to collect VPC discovered count: {}", e.getMessage());
            metrics.put("vpc.count", 0.0);
        }

        try {
            metrics.put("rds.count", (double) rdsProvider.describeDbInstances(region, accountId).size());
        } catch (Exception e) {
            log.warn("Unable to collect RDS discovered count: {}", e.getMessage());
            metrics.put("rds.count", 0.0);
        }

        return metrics;
    }

    private String resolveAccountId() {
        try {
            return stsIdentityProvider.getCallerIdentity().accountId();
        } catch (Exception e) {
            log.warn("Failed to resolve AWS STS caller identity, using fallback: {}", e.getMessage());
            return "351405419700";
        }
    }
}
