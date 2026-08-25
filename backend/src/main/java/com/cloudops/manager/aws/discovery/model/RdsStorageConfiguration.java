package com.cloudops.manager.aws.discovery.model;

public record RdsStorageConfiguration(
    Integer allocatedStorageGb,
    Integer maxAllocatedStorageGb,
    String storageType,
    Integer iops,
    Integer storageThroughput,
    Boolean storageEncrypted,
    String kmsKeyId
) {}