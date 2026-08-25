package com.cloudops.manager.aws.discovery.model;

public record RdsMonitoringConfiguration(
    Boolean enhancedMonitoringEnabled,
    Integer monitoringInterval,
    String monitoringRoleArn
) {}