package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record Ec2DetailResource(
    String instanceId,
    String name,
    String arn,
    String accountId,
    String region,
    String instanceType,
    String architecture,
    String platform,
    String platformDetails,
    String amiId,
    String kernelId,
    Instant launchTime,
    String instanceState,
    String stateReason,
    String instanceLifecycle,
    String monitoringState,
    String availabilityZone,
    String placementGroup,
    String tenancy,
    String vpcId,
    String subnetId,
    String privateIp,
    String publicIp,
    String privateDnsName,
    String publicDnsName,
    List<Ec2EbsAttachment> ebsVolumes,
    List<Ec2NetworkInterfaceDetail> networkInterfaces,
    Map<String, String> tags,
    Instant discoveredAt
) {}