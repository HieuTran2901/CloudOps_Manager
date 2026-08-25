package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;

public record Ec2EbsAttachment(
    String volumeId,
    String deviceName,
    Integer sizeGiB,
    String volumeType,
    Integer iops,
    Integer throughput,
    Boolean encrypted,
    String state,
    String availabilityZone,
    Instant attachTime,
    Boolean deleteOnTermination
) {}