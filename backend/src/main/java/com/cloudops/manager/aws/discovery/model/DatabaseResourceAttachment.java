package com.cloudops.manager.aws.discovery.model;

public record DatabaseResourceAttachment(
    String dbInstanceIdentifier,
    String engine,
    String status
) {}