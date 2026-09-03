package com.cloudops.manager.aws.quota;

import com.cloudops.manager.aws.discovery.model.Ec2InstanceResource;
import com.cloudops.manager.aws.discovery.model.RdsInstanceResource;
import com.cloudops.manager.aws.discovery.model.VpcResource;
import com.cloudops.manager.aws.discovery.provider.Ec2Provider;
import com.cloudops.manager.aws.discovery.provider.RdsProvider;
import com.cloudops.manager.aws.discovery.provider.VpcProvider;
import com.cloudops.manager.aws.quota.model.QuotaStatus;
import com.cloudops.manager.aws.quota.model.QuotaUtilizationReport;
import com.cloudops.manager.aws.quota.model.ServiceQuotaItem;
import com.cloudops.manager.aws.quota.provider.ServiceQuotasProvider;
import com.cloudops.manager.aws.quota.service.Ec2VcpuResolver;
import com.cloudops.manager.aws.quota.service.ServiceQuotasService;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.provider.StsIdentityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceQuotasServiceTest {

    @Mock
    private ServiceQuotasProvider serviceQuotasProvider;

    @Mock
    private Ec2Provider ec2Provider;

    @Mock
    private VpcProvider vpcProvider;

    @Mock
    private RdsProvider rdsProvider;

    @Mock
    private StsIdentityProvider stsIdentityProvider;

    private ServiceQuotasService serviceQuotasService;

    @BeforeEach
    void setUp() {
        serviceQuotasService = new ServiceQuotasService(
                serviceQuotasProvider,
                ec2Provider,
                vpcProvider,
                rdsProvider,
                stsIdentityProvider,
                "ap-southeast-2"
        );
    }

    @Test
    @DisplayName("EC2 vCPU Resolver accurately parses and sums vCPUs across instance types")
    void testEc2VcpuResolverAccurateSizing() {
        assertEquals(2, Ec2VcpuResolver.resolveVcpus("t3.micro"));
        assertEquals(1, Ec2VcpuResolver.resolveVcpus("t2.nano"));
        assertEquals(16, Ec2VcpuResolver.resolveVcpus("c5.4xlarge"));
        assertEquals(32, Ec2VcpuResolver.resolveVcpus("m5.8xlarge"));
        assertEquals(64, Ec2VcpuResolver.resolveVcpus("r5.16xlarge"));
        assertEquals(2, Ec2VcpuResolver.resolveVcpus("unknown.custom"));

        Ec2InstanceResource inst1 = new Ec2InstanceResource(
                "i-1", null, "app-1", "ap-southeast-2", "351405419700", "running",
                null, null, Instant.now(), "c5.4xlarge", null, null, null, null, null, null, null
        );
        Ec2InstanceResource inst2 = new Ec2InstanceResource(
                "i-2", null, "app-2", "ap-southeast-2", "351405419700", "running",
                null, null, Instant.now(), "c5.4xlarge", null, null, null, null, null, null, null
        );

        // 2 x c5.4xlarge = 32 vCPUs
        double totalVcpus = Ec2VcpuResolver.sumTotalVcpus(List.of(inst1, inst2));
        assertEquals(32.0, totalVcpus);

        // Utilization against 32 quota limit = 100.00% -> CRITICAL
        Double util100 = ServiceQuotasService.calculateUtilization(totalVcpus, 32.0);
        assertEquals(100.00, util100);
        assertEquals(QuotaStatus.CRITICAL, ServiceQuotasService.classifyStatus(util100));

        // Utilization against 64 quota limit = 50.00% -> NORMAL
        Double util50 = ServiceQuotasService.calculateUtilization(totalVcpus, 64.0);
        assertEquals(50.00, util50);
        assertEquals(QuotaStatus.NORMAL, ServiceQuotasService.classifyStatus(util50));
    }

    @Test
    @DisplayName("Boundary threshold tests for utilization classification with UNKNOWN support")
    void testBoundaryThresholdClassifications() {
        // null utilization -> UNKNOWN
        assertEquals(QuotaStatus.UNKNOWN, ServiceQuotasService.classifyStatus(null));

        // 79.99% -> NORMAL
        assertEquals(QuotaStatus.NORMAL, ServiceQuotasService.classifyStatus(79.99));

        // 80.00% -> WARNING
        assertEquals(QuotaStatus.WARNING, ServiceQuotasService.classifyStatus(80.00));

        // 89.99% -> WARNING
        assertEquals(QuotaStatus.WARNING, ServiceQuotasService.classifyStatus(89.99));

        // 90.00% -> CRITICAL
        assertEquals(QuotaStatus.CRITICAL, ServiceQuotasService.classifyStatus(90.00));

        // 100.00% -> CRITICAL
        assertEquals(QuotaStatus.CRITICAL, ServiceQuotasService.classifyStatus(100.00));

        // >100% -> CRITICAL
        assertEquals(QuotaStatus.CRITICAL, ServiceQuotasService.classifyStatus(125.50));
    }

    @Test
    @DisplayName("Utilization calculation edge cases and UNKNOWN handling")
    void testCalculateUtilizationEdgeCases() {
        // Normal case: 82 out of 100 -> 82.00%
        assertEquals(82.00, ServiceQuotasService.calculateUtilization(82.0, 100.0));

        // 0 usage out of 100 -> 0.00%
        assertEquals(0.00, ServiceQuotasService.calculateUtilization(0.0, 100.0));

        // Rounding: 2 out of 3 -> 66.67%
        assertEquals(66.67, ServiceQuotasService.calculateUtilization(2.0, 3.0));

        // Null current usage -> null (mapped to UNKNOWN)
        assertNull(ServiceQuotasService.calculateUtilization(null, 100.0));

        // Null applied limit -> null (mapped to UNKNOWN)
        assertNull(ServiceQuotasService.calculateUtilization(50.0, null));

        // Zero limit -> null (safe division by zero -> UNKNOWN)
        assertNull(ServiceQuotasService.calculateUtilization(50.0, 0.0));

        // Negative limit -> null
        assertNull(ServiceQuotasService.calculateUtilization(50.0, -10.0));

        // Negative usage clamped to 0 -> 0.00%
        assertEquals(0.00, ServiceQuotasService.calculateUtilization(-5.0, 100.0));
    }

    @Test
    @DisplayName("Report aggregation correlates vCPU usage, flags UNKNOWN, and enforces summary invariant")
    void testGetQuotaUtilizationReportAggregation() {
        when(stsIdentityProvider.getCallerIdentity()).thenReturn(
                new CallerIdentity("351405419700", "arn:aws:iam::351405419700:user/test", "AIDATEST")
        );

        ServiceQuotaItem vpcQuota = new ServiceQuotaItem(
                "vpc", "Amazon Virtual Private Cloud", "L-F678F13E", "VPCs per Region",
                5.0, null, null, QuotaStatus.UNKNOWN, "ap-southeast-2", "UNAVAILABLE", "Count", true, Instant.now()
        );

        ServiceQuotaItem ec2Quota = new ServiceQuotaItem(
                "ec2", "Amazon Elastic Compute Cloud", "L-1216C47A", "Running On-Demand Standard instances",
                32.0, null, null, QuotaStatus.UNKNOWN, "ap-southeast-2", "UNAVAILABLE", "vCPU", true, Instant.now()
        );

        ServiceQuotaItem unmappedQuota = new ServiceQuotaItem(
                "iam", "AWS Identity and Access Management", "L-SOMEQUOTA", "Roles per account",
                1000.0, null, null, QuotaStatus.UNKNOWN, "ap-southeast-2", "UNAVAILABLE", "Count", false, Instant.now()
        );

        when(serviceQuotasProvider.listCoreServiceQuotas("ap-southeast-2", "351405419700"))
                .thenReturn(List.of(vpcQuota, ec2Quota, unmappedQuota));

        // 4 VPCs out of 5 = 80.00% -> WARNING
        VpcResource vpcMock = mock(VpcResource.class);
        when(vpcProvider.describeVpcs("ap-southeast-2", "351405419700"))
                .thenReturn(List.of(vpcMock, vpcMock, vpcMock, vpcMock));

        // 2 x c5.4xlarge EC2 instances = 32 vCPUs out of 32 = 100.00% -> CRITICAL
        Ec2InstanceResource inst1 = new Ec2InstanceResource(
                "i-1", null, "app-1", "ap-southeast-2", "351405419700", "running",
                null, null, Instant.now(), "c5.4xlarge", null, null, null, null, null, null, null
        );
        Ec2InstanceResource inst2 = new Ec2InstanceResource(
                "i-2", null, "app-2", "ap-southeast-2", "351405419700", "running",
                null, null, Instant.now(), "c5.4xlarge", null, null, null, null, null, null, null
        );
        when(ec2Provider.describeInstances("ap-southeast-2", "351405419700"))
                .thenReturn(List.of(inst1, inst2));

        when(rdsProvider.describeDbInstances("ap-southeast-2", "351405419700"))
                .thenReturn(Collections.emptyList());

        QuotaUtilizationReport report = serviceQuotasService.getQuotaUtilizationReport("ap-southeast-2");

        assertNotNull(report);
        assertEquals("351405419700", report.accountId());
        assertEquals("ap-southeast-2", report.region());
        assertEquals(3, report.totalQuotasTracked());
        assertEquals(0, report.normalCount());
        assertEquals(1, report.warningCount());
        assertEquals(1, report.criticalCount());
        assertEquals(1, report.unknownCount());
        assertEquals(100.00, report.highestUtilizationPercentage());

        // Invariant check: normal + warning + critical + unknown == totalQuotasTracked == quotas.size()
        assertEquals(report.totalQuotasTracked(), report.quotas().size());
        assertEquals(report.totalQuotasTracked(), report.normalCount() + report.warningCount() + report.criticalCount() + report.unknownCount());

        ServiceQuotaItem vpcResult = report.quotas().stream()
                .filter(q -> "L-F678F13E".equals(q.quotaCode())).findFirst().orElseThrow();
        assertEquals(4.0, vpcResult.currentUsage());
        assertEquals(80.00, vpcResult.utilizationPercentage());
        assertEquals(QuotaStatus.WARNING, vpcResult.status());
        assertEquals("VPC_DISCOVERY", vpcResult.usageSource());

        ServiceQuotaItem ec2Result = report.quotas().stream()
                .filter(q -> "L-1216C47A".equals(q.quotaCode())).findFirst().orElseThrow();
        assertEquals(32.0, ec2Result.currentUsage());
        assertEquals(100.00, ec2Result.utilizationPercentage());
        assertEquals(QuotaStatus.CRITICAL, ec2Result.status());
        assertEquals("EC2_VCPU_DISCOVERY", ec2Result.usageSource());

        ServiceQuotaItem unmappedResult = report.quotas().stream()
                .filter(q -> "L-SOMEQUOTA".equals(q.quotaCode())).findFirst().orElseThrow();
        assertNull(unmappedResult.currentUsage());
        assertNull(unmappedResult.utilizationPercentage());
        assertEquals(QuotaStatus.UNKNOWN, unmappedResult.status());
        assertEquals("UNAVAILABLE", unmappedResult.usageSource());
    }
}
