package com.cloudops.manager.aws.quota.provider;

import com.cloudops.manager.aws.discovery.config.AwsClientFactory;
import com.cloudops.manager.aws.quota.model.QuotaStatus;
import com.cloudops.manager.aws.quota.model.ServiceQuotaItem;
import com.cloudops.manager.common.exception.AwsErrorTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.servicequotas.ServiceQuotasClient;
import software.amazon.awssdk.services.servicequotas.model.ListServiceQuotasRequest;
import software.amazon.awssdk.services.servicequotas.model.ServiceQuota;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AwsServiceQuotasProvider implements ServiceQuotasProvider {

    private static final Logger log = LoggerFactory.getLogger(AwsServiceQuotasProvider.class);

    private static final List<String> CORE_SERVICES = List.of("ec2", "vpc", "rds");

    private final AwsClientFactory awsClientFactory;
    private final Map<String, ServiceQuotasClient> clientCache = new ConcurrentHashMap<>();

    public AwsServiceQuotasProvider(AwsClientFactory awsClientFactory) {
        this.awsClientFactory = awsClientFactory;
    }

    private ServiceQuotasClient getClient(String region) {
        return clientCache.computeIfAbsent(region, awsClientFactory::getServiceQuotasClient);
    }

    @Override
    public List<ServiceQuotaItem> listServiceQuotas(String serviceCode, String region, String accountId) {
        log.info("Querying AWS Service Quotas for service: {}, region: {}, account: {}", serviceCode, region, accountId);
        List<ServiceQuotaItem> results = new ArrayList<>();
        Instant evaluatedAt = Instant.now();

        try {
            ServiceQuotasClient client = getClient(region);
            var paginator = client.listServiceQuotasPaginator(
                    ListServiceQuotasRequest.builder()
                            .serviceCode(serviceCode)
                            .build()
            );

            for (var response : paginator) {
                for (ServiceQuota quota : response.quotas()) {
                    results.add(mapToItem(quota, region, evaluatedAt));
                }
            }

            log.info("Retrieved {} service quotas for service: {} in region: {}", results.size(), serviceCode, region);
            return results;
        } catch (Exception e) {
            log.warn("Failed to fetch quotas for service: {} in region: {}: {}", serviceCode, region, e.getMessage());
            throw AwsErrorTranslator.translate("ServiceQuotas:ListServiceQuotas:" + serviceCode, e, log);
        }
    }

    @Override
    public List<ServiceQuotaItem> listCoreServiceQuotas(String region, String accountId) {
        log.info("Querying core AWS Service Quotas for region: {}, account: {}", region, accountId);
        List<ServiceQuotaItem> aggregated = new ArrayList<>();
        for (String serviceCode : CORE_SERVICES) {
            try {
                aggregated.addAll(listServiceQuotas(serviceCode, region, accountId));
            } catch (Exception e) {
                log.warn("Non-fatal error querying quotas for service {}: {}", serviceCode, e.getMessage());
            }
        }
        return aggregated;
    }

    private ServiceQuotaItem mapToItem(ServiceQuota quota, String region, Instant evaluatedAt) {
        Double limit = quota.value() != null ? quota.value() : null;
        boolean adjustable = Boolean.TRUE.equals(quota.adjustable());

        return new ServiceQuotaItem(
                quota.serviceCode(),
                quota.serviceName(),
                quota.quotaCode(),
                quota.quotaName(),
                limit,
                null,                  // currentUsage will be correlated by ServiceQuotasService
                null,                  // utilizationPercentage computed by ServiceQuotasService
                QuotaStatus.UNKNOWN,   // status classified by ServiceQuotasService
                region,
                "UNAVAILABLE",         // default usageSource until correlated
                quota.unit() != null ? quota.unit() : "Count",
                adjustable,
                evaluatedAt
        );
    }
}
