package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.CloudResourceType;
import com.cloudops.manager.aws.discovery.model.RdsBackupConfiguration;
import com.cloudops.manager.aws.discovery.model.RdsDetailResource;
import com.cloudops.manager.aws.discovery.model.RdsInstanceResource;
import com.cloudops.manager.aws.discovery.model.RdsMaintenanceConfiguration;
import com.cloudops.manager.aws.discovery.model.RdsMonitoringConfiguration;
import com.cloudops.manager.aws.discovery.model.RdsNetworkConfiguration;
import com.cloudops.manager.aws.discovery.model.RdsOptionGroupInfo;
import com.cloudops.manager.aws.discovery.model.RdsParameterGroupInfo;
import com.cloudops.manager.aws.discovery.model.RdsStorageConfiguration;
import com.cloudops.manager.common.exception.AwsErrorTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DBSubnetGroup;
import software.amazon.awssdk.services.rds.model.DbInstanceNotFoundException;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.RdsException;
import software.amazon.awssdk.services.rds.model.Subnet;
import software.amazon.awssdk.services.rds.model.Tag;
import software.amazon.awssdk.services.rds.model.VpcSecurityGroupMembership;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AwsRdsProvider implements RdsProvider {

    private static final Logger log = LoggerFactory.getLogger(AwsRdsProvider.class);
    private final RdsClient rdsClient;

    public AwsRdsProvider(RdsClient rdsClient) {
        this.rdsClient = rdsClient;
    }

    @Override
    public List<RdsInstanceResource> describeDbInstances(String region, String accountId) {
        log.info("Discovering RDS instances for account: {}, region: {}", accountId, region);
        List<RdsInstanceResource> results = new ArrayList<>();
        Instant discoveredAt = Instant.now();

        try {
            var paginator = rdsClient.describeDBInstancesPaginator(DescribeDbInstancesRequest.builder().build());

            for (var response : paginator) {
                for (DBInstance db : response.dbInstances()) {
                    Map<String, String> tags = db.hasTagList()
                            ? db.tagList().stream().collect(Collectors.toMap(Tag::key, Tag::value, (k1, k2) -> k1))
                            : Collections.emptyMap();

                    String endpointAddress = db.endpoint() != null ? db.endpoint().address() : null;
                    Integer port = db.endpoint() != null ? db.endpoint().port() : null;
                    String subnetGroup = db.dbSubnetGroup() != null ? db.dbSubnetGroup().dbSubnetGroupName() : null;
                    String vpcId = db.dbSubnetGroup() != null ? db.dbSubnetGroup().vpcId() : null;

                    results.add(new RdsInstanceResource(
                            db.dbInstanceIdentifier(),
                            CloudResourceType.RDS_INSTANCE,
                            db.dbInstanceIdentifier(),
                            region,
                            accountId,
                            db.dbInstanceStatus() != null ? db.dbInstanceStatus().toUpperCase() : "UNKNOWN",
                            db.dbInstanceArn(),
                            tags,
                            discoveredAt,
                            db.engine(),
                            db.engineVersion(),
                            db.dbInstanceClass(),
                            endpointAddress,
                            port,
                            db.availabilityZone(),
                            subnetGroup,
                            vpcId,
                            db.publiclyAccessible(),
                            db.allocatedStorage()
                    ));
                }
            }
            log.info("Discovered {} RDS instances in region: {}", results.size(), region);
            return results;
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("RDS:DescribeDBInstances", e, log);
        }
    }

    @Override
    public Optional<RdsDetailResource> getDbInstance(String dbInstanceIdentifier, String region, String accountId) {
        log.info("Inspecting RDS instance: {} for account: {}, region: {}", dbInstanceIdentifier, accountId, region);
        try {
            DescribeDbInstancesResponse response = rdsClient.describeDBInstances(
                    DescribeDbInstancesRequest.builder().dbInstanceIdentifier(dbInstanceIdentifier).build()
            );

            if (!response.hasDbInstances() || response.dbInstances().isEmpty()) {
                return Optional.empty();
            }

            DBInstance db = response.dbInstances().get(0);
            return Optional.of(mapToDetail(db, region, accountId));
        } catch (DbInstanceNotFoundException e) {
            log.info("RDS instance {} not found", dbInstanceIdentifier);
            return Optional.empty();
        } catch (RdsException e) {
            if (e.statusCode() == 404 || "DBInstanceNotFound".equalsIgnoreCase(e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "")) {
                log.info("RDS instance {} does not exist (404)", dbInstanceIdentifier);
                return Optional.empty();
            }
            throw AwsErrorTranslator.translate("RDS:DescribeDBInstance:" + dbInstanceIdentifier, e, log);
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("RDS:DescribeDBInstance:" + dbInstanceIdentifier, e, log);
        }
    }

    private RdsDetailResource mapToDetail(DBInstance db, String region, String accountId) {
        RdsStorageConfiguration storage = new RdsStorageConfiguration(
                db.allocatedStorage(),
                db.maxAllocatedStorage(),
                db.storageType(),
                db.iops(),
                db.storageThroughput(),
                db.storageEncrypted(),
                db.kmsKeyId()
        );

        RdsBackupConfiguration backup = new RdsBackupConfiguration(
                db.backupRetentionPeriod(),
                db.preferredBackupWindow(),
                db.latestRestorableTime(),
                db.copyTagsToSnapshot()
        );

        String vpcId = null;
        String dbSubnetGroupName = null;
        List<String> subnetIds = new ArrayList<>();
        List<String> subnetAzs = new ArrayList<>();
        if (db.dbSubnetGroup() != null) {
            DBSubnetGroup sng = db.dbSubnetGroup();
            vpcId = sng.vpcId();
            dbSubnetGroupName = sng.dbSubnetGroupName();
            if (sng.hasSubnets()) {
                for (Subnet s : sng.subnets()) {
                    subnetIds.add(s.subnetIdentifier());
                    if (s.subnetAvailabilityZone() != null && s.subnetAvailabilityZone().name() != null) {
                        subnetAzs.add(s.subnetAvailabilityZone().name());
                    }
                }
            }
        }

        List<String> sgIds = new ArrayList<>();
        List<String> sgStatuses = new ArrayList<>();
        if (db.hasVpcSecurityGroups()) {
            for (VpcSecurityGroupMembership m : db.vpcSecurityGroups()) {
                sgIds.add(m.vpcSecurityGroupId());
                sgStatuses.add(m.status());
            }
        }

        String endpointAddress = db.endpoint() != null ? db.endpoint().address() : null;
        Integer endpointPort = db.endpoint() != null ? db.endpoint().port() : null;

        RdsNetworkConfiguration network = new RdsNetworkConfiguration(
                vpcId,
                dbSubnetGroupName,
                subnetIds,
                subnetAzs,
                sgIds,
                sgStatuses,
                db.publiclyAccessible(),
                endpointAddress,
                endpointPort
        );

        boolean hasPending = db.pendingModifiedValues() != null;
        RdsMaintenanceConfiguration maintenance = new RdsMaintenanceConfiguration(
                db.preferredMaintenanceWindow(),
                db.autoMinorVersionUpgrade(),
                hasPending
        );

        boolean enhancedMon = db.monitoringInterval() != null && db.monitoringInterval() > 0;
        RdsMonitoringConfiguration monitoring = new RdsMonitoringConfiguration(
                enhancedMon,
                db.monitoringInterval(),
                db.monitoringRoleArn()
        );

        List<RdsParameterGroupInfo> paramGroups = new ArrayList<>();
        if (db.hasDbParameterGroups()) {
            for (var pg : db.dbParameterGroups()) {
                paramGroups.add(new RdsParameterGroupInfo(pg.dbParameterGroupName(), pg.parameterApplyStatus()));
            }
        }

        List<RdsOptionGroupInfo> optionGroups = new ArrayList<>();
        if (db.hasOptionGroupMemberships()) {
            for (var og : db.optionGroupMemberships()) {
                optionGroups.add(new RdsOptionGroupInfo(og.optionGroupName(), og.status()));
            }
        }

        Map<String, String> tags = db.hasTagList()
                ? db.tagList().stream().collect(Collectors.toMap(Tag::key, Tag::value, (k1, k2) -> k1))
                : Collections.emptyMap();

        String secondaryAz = db.secondaryAvailabilityZone();

        return new RdsDetailResource(
                db.dbInstanceIdentifier(),
                db.dbInstanceArn(),
                accountId,
                region,
                db.engine(),
                db.engineVersion(),
                db.dbInstanceClass(),
                db.dbInstanceStatus() != null ? db.dbInstanceStatus().toUpperCase() : "UNKNOWN",
                db.availabilityZone(),
                db.multiAZ(),
                secondaryAz,
                db.promotionTier(),
                db.deletionProtection(),
                db.iamDatabaseAuthenticationEnabled(),
                db.caCertificateIdentifier(),
                storage,
                backup,
                network,
                maintenance,
                monitoring,
                paramGroups,
                optionGroups,
                tags,
                Instant.now()
        );
    }
}