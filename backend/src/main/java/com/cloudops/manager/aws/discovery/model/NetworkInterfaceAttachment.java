package com.cloudops.manager.aws.discovery.model;

public record NetworkInterfaceAttachment(
    String networkInterfaceId,
    String subnetId,
    String vpcId,
    String privateIpAddress,
    String interfaceType,
    String attachmentStatus
) {}