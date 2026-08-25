package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.Map;

public record RdsInstanceResource(
    String resourceId,
    CloudResourceType resourceType,
    String name,
    String region,
    String accountId,
    String status,
    String arn,
    Map<String, String> tags,
    Instant discoveredAt,
    String engine,
    String engineVersion,
    String instanceClass,
    String endpoint,
    Integer port,
    String availabilityZone,
    String dbSubnetGroup,
    String vpcId,
    Boolean publiclyAccessible,
    Integer allocatedStorageGb
) implements CloudResource {}