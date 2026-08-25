package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.CloudResourceType;
import com.cloudops.manager.aws.discovery.model.RdsDetailResource;
import com.cloudops.manager.aws.discovery.model.RdsInstanceResource;
import com.cloudops.manager.common.exception.AwsAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DBParameterGroupStatus;
import software.amazon.awssdk.services.rds.model.DBSubnetGroup;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.Endpoint;
import software.amazon.awssdk.services.rds.model.OptionGroupMembership;
import software.amazon.awssdk.services.rds.model.PendingModifiedValues;
import software.amazon.awssdk.services.rds.model.RdsException;
import software.amazon.awssdk.services.rds.model.Subnet;
import software.amazon.awssdk.services.rds.model.Tag;
import software.amazon.awssdk.services.rds.model.VpcSecurityGroupMembership;
import software.amazon.awssdk.services.rds.paginators.DescribeDBInstancesIterable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsRdsProviderTest {

    @Mock
    private RdsClient rdsClient;

    private AwsRdsProvider rdsProvider;

    @BeforeEach
    void setUp() {
        rdsProvider = new AwsRdsProvider(rdsClient);
    }

    @Test
    @DisplayName("Should successfully describe RDS instances via paginator")
    void shouldDescribeRdsInstances() {
        DBInstance db = DBInstance.builder()
                .dbInstanceIdentifier("postgres-prod")
                .dbInstanceArn("arn:aws:rds:us-east-1:123456789012:db:postgres-prod")
                .engine("postgres")
                .engineVersion("15.3")
                .dbInstanceClass("db.t4g.medium")
                .dbInstanceStatus("available")
                .availabilityZone("us-east-1a")
                .allocatedStorage(50)
                .publiclyAccessible(false)
                .endpoint(Endpoint.builder().address("postgres.internal").port(5432).build())
                .dbSubnetGroup(DBSubnetGroup.builder().dbSubnetGroupName("main-subnet").vpcId("vpc-123").build())
                .tagList(Tag.builder().key("Env").value("Prod").build())
                .build();

        DescribeDbInstancesResponse page = DescribeDbInstancesResponse.builder().dbInstances(db).build();
        DescribeDBInstancesIterable mockPaginator = mock(DescribeDBInstancesIterable.class);
        when(mockPaginator.iterator()).thenReturn(List.of(page).iterator());
        when(rdsClient.describeDBInstancesPaginator(any(DescribeDbInstancesRequest.class))).thenReturn(mockPaginator);

        List<RdsInstanceResource> instances = rdsProvider.describeDbInstances("us-east-1", "123456789012");

        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).resourceId()).isEqualTo("postgres-prod");
        assertThat(instances.get(0).resourceType()).isEqualTo(CloudResourceType.RDS_INSTANCE);
        assertThat(instances.get(0).engine()).isEqualTo("postgres");
        assertThat(instances.get(0).status()).isEqualTo("AVAILABLE");
        assertThat(instances.get(0).tags()).containsEntry("Env", "Prod");
    }

    @Test
    @DisplayName("Should successfully inspect RDS DB instance details with full configuration")
    void shouldGetRdsInstanceDetail() {
        DBInstance db = DBInstance.builder()
                .dbInstanceIdentifier("mysql-db")
                .dbInstanceArn("arn:aws:rds:us-east-1:123456789012:db:mysql-db")
                .engine("mysql")
                .engineVersion("8.0.32")
                .dbInstanceClass("db.r6g.large")
                .dbInstanceStatus("available")
                .availabilityZone("us-east-1b")
                .multiAZ(true)
                .deletionProtection(true)
                .iamDatabaseAuthenticationEnabled(true)
                .caCertificateIdentifier("rds-ca-2019")
                .allocatedStorage(100)
                .maxAllocatedStorage(500)
                .storageType("gp3")
                .iops(3000)
                .storageThroughput(125)
                .storageEncrypted(true)
                .kmsKeyId("arn:aws:kms:us-east-1:123456789012:key/abc-123")
                .backupRetentionPeriod(7)
                .preferredBackupWindow("03:00-04:00")
                .copyTagsToSnapshot(true)
                .preferredMaintenanceWindow("Sun:05:00-Sun:06:00")
                .autoMinorVersionUpgrade(true)
                .pendingModifiedValues(PendingModifiedValues.builder().allocatedStorage(200).build())
                .monitoringInterval(60)
                .monitoringRoleArn("arn:aws:iam::123456789012:role/rds-monitoring-role")
                .endpoint(Endpoint.builder().address("mysql.rds.amazonaws.com").port(3306).build())
                .publiclyAccessible(false)
                .dbSubnetGroup(DBSubnetGroup.builder()
                        .dbSubnetGroupName("dbsubnet-1")
                        .vpcId("vpc-999")
                        .subnets(Subnet.builder().subnetIdentifier("subnet-a").build())
                        .build())
                .vpcSecurityGroups(VpcSecurityGroupMembership.builder().vpcSecurityGroupId("sg-111").status("active").build())
                .dbParameterGroups(DBParameterGroupStatus.builder().dbParameterGroupName("default.mysql8.0").parameterApplyStatus("in-sync").build())
                .optionGroupMemberships(OptionGroupMembership.builder().optionGroupName("default:mysql-8-0").status("in-sync").build())
                .tagList(Tag.builder().key("Project").value("CloudOps").build())
                .build();

        when(rdsClient.describeDBInstances(any(DescribeDbInstancesRequest.class)))
                .thenReturn(DescribeDbInstancesResponse.builder().dbInstances(db).build());

        Optional<RdsDetailResource> result = rdsProvider.getDbInstance("mysql-db", "us-east-1", "123456789012");

        assertThat(result).isPresent();
        RdsDetailResource d = result.get();
        assertThat(d.dbInstanceIdentifier()).isEqualTo("mysql-db");
        assertThat(d.engine()).isEqualTo("mysql");
        assertThat(d.multiAz()).isTrue();
        assertThat(d.deletionProtection()).isTrue();
        assertThat(d.iamDatabaseAuthenticationEnabled()).isTrue();
        assertThat(d.storage().allocatedStorageGb()).isEqualTo(100);
        assertThat(d.storage().storageEncrypted()).isTrue();
        assertThat(d.storage().kmsKeyId()).contains("abc-123");
        assertThat(d.backup().backupRetentionPeriod()).isEqualTo(7);
        assertThat(d.network().vpcId()).isEqualTo("vpc-999");
        assertThat(d.network().securityGroupIds()).contains("sg-111");
        assertThat(d.maintenance().autoMinorVersionUpgrade()).isTrue();
        assertThat(d.maintenance().pendingModifiedValues()).isTrue();
        assertThat(d.monitoring().enhancedMonitoringEnabled()).isTrue();
        assertThat(d.monitoring().monitoringInterval()).isEqualTo(60);
        assertThat(d.parameterGroups()).hasSize(1);
        assertThat(d.tags()).containsEntry("Project", "CloudOps");
    }

    @Test
    @DisplayName("Should return Optional.empty() when RDS instance is not found")
    void shouldReturnEmptyWhenNotFound() {
        when(rdsClient.describeDBInstances(any(DescribeDbInstancesRequest.class)))
                .thenThrow(DbInstanceNotFoundException.builder().message("DB instance not found").build());

        Optional<RdsDetailResource> result = rdsProvider.getDbInstance("missing-db", "us-east-1", "123456789012");

        assertThat(result).isEmpty();
    }
}