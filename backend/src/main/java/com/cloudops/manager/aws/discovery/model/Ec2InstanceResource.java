package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.Map;

public record Ec2InstanceResource(
    String resourceId,
    CloudResourceType resourceType,
    String name,
    String region,
    String accountId,
    String status,
    String arn,
    Map<String, String> tags,
    Instant discoveredAt,
    String instanceType,
    String privateIp,
    String publicIp,
    String vpcId,
    String subnetId,
    String availabilityZone,
    String amiId,
    Instant launchTime
) implements CloudResource {}