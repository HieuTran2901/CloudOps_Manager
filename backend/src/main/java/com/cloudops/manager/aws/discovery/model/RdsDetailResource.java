package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RdsDetailResource(
    String dbInstanceIdentifier,
    String arn,
    String accountId,
    String region,
    String engine,
    String engineVersion,
    String dbInstanceClass,
    String dbInstanceStatus,
    String availabilityZone,
    Boolean multiAz,
    String preferredAvailabilityZone,
    Integer promotionTier,
    Boolean deletionProtection,
    Boolean iamDatabaseAuthenticationEnabled,
    String caCertificateIdentifier,
    RdsStorageConfiguration storage,
    RdsBackupConfiguration backup,
    RdsNetworkConfiguration network,
    RdsMaintenanceConfiguration maintenance,
    RdsMonitoringConfiguration monitoring,
    List<RdsParameterGroupInfo> parameterGroups,
    List<RdsOptionGroupInfo> optionGroups,
    Map<String, String> tags,
    Instant discoveredAt
) {}