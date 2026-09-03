package com.cloudops.manager.aws.quota;

import com.cloudops.manager.aws.discovery.config.AwsClientFactory;
import com.cloudops.manager.aws.quota.model.ServiceQuotaItem;
import com.cloudops.manager.aws.quota.provider.AwsServiceQuotasProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.servicequotas.ServiceQuotasClient;
import software.amazon.awssdk.services.servicequotas.model.ListServiceQuotasRequest;
import software.amazon.awssdk.services.servicequotas.model.ListServiceQuotasResponse;
import software.amazon.awssdk.services.servicequotas.model.ServiceQuota;
import software.amazon.awssdk.services.servicequotas.paginators.ListServiceQuotasIterable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AwsServiceQuotasProviderTest {

    @Mock
    private AwsClientFactory awsClientFactory;

    @Mock
    private ServiceQuotasClient serviceQuotasClientAp;

    @Mock
    private ServiceQuotasClient serviceQuotasClientUs;

    @Mock
    private ListServiceQuotasIterable paginator;

    private AwsServiceQuotasProvider provider;

    @BeforeEach
    void setUp() {
        provider = new AwsServiceQuotasProvider(awsClientFactory);
    }

    @Test
    @DisplayName("Provider maps AWS SDK ServiceQuota items and resolves multi-region clients dynamically")
    void testListServiceQuotasMappingAndMultiRegion() {
        ServiceQuota quota = ServiceQuota.builder()
                .serviceCode("ec2")
                .serviceName("Amazon Elastic Compute Cloud")
                .quotaCode("L-1216C47A")
                .quotaName("Running On-Demand Standard instances")
                .value(32.0)
                .unit("vCPU")
                .adjustable(true)
                .build();

        ListServiceQuotasResponse page = ListServiceQuotasResponse.builder()
                .quotas(List.of(quota))
                .build();

        when(awsClientFactory.getServiceQuotasClient("ap-southeast-2")).thenReturn(serviceQuotasClientAp);
        when(awsClientFactory.getServiceQuotasClient("us-east-1")).thenReturn(serviceQuotasClientUs);

        when(serviceQuotasClientAp.listServiceQuotasPaginator(any(ListServiceQuotasRequest.class)))
                .thenReturn(paginator);
        when(serviceQuotasClientUs.listServiceQuotasPaginator(any(ListServiceQuotasRequest.class)))
                .thenReturn(paginator);
        when(paginator.iterator()).thenReturn(List.of(page).iterator(), List.of(page).iterator());

        // Test ap-southeast-2
        List<ServiceQuotaItem> itemsAp = provider.listServiceQuotas("ec2", "ap-southeast-2", "351405419700");
        assertNotNull(itemsAp);
        assertEquals(1, itemsAp.size());
        assertEquals("ap-southeast-2", itemsAp.get(0).region());

        // Test us-east-1
        List<ServiceQuotaItem> itemsUs = provider.listServiceQuotas("ec2", "us-east-1", "351405419700");
        assertNotNull(itemsUs);
        assertEquals(1, itemsUs.size());
        assertEquals("us-east-1", itemsUs.get(0).region());

        verify(awsClientFactory).getServiceQuotasClient("ap-southeast-2");
        verify(awsClientFactory).getServiceQuotasClient("us-east-1");
    }
}
