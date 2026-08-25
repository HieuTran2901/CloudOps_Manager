package com.cloudops.manager.aws.discovery.model;

public record ComputeResourceAttachment(
    String instanceId,
    String instanceName,
    String instanceType,
    String state
) {}