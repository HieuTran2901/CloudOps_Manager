package com.cloudops.manager.aws.discovery.model;

public record S3EncryptionConfiguration(
    String status,
    Boolean enabled,
    String algorithm,
    String kmsMasterKeyId,
    Boolean bucketKeyEnabled
) {}