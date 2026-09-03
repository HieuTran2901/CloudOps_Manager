package com.cloudops.manager.aws.quota.provider;

import com.cloudops.manager.aws.quota.model.ServiceQuotaItem;
import java.util.List;

public interface ServiceQuotasProvider {

    /**
     * Lists AWS Service Quotas for a specific service in the given region and account.
     */
    List<ServiceQuotaItem> listServiceQuotas(String serviceCode, String region, String accountId);

    /**
     * Retrieves key curated service quotas across core AWS services (e.g. EC2, VPC, RDS).
     */
    List<ServiceQuotaItem> listCoreServiceQuotas(String region, String accountId);
}
