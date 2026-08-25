package com.cloudops.manager.aws.discovery.model;

public record S3VersioningConfiguration(
    String status,
    String versioningStatus,
    String mfaDelete
) {}