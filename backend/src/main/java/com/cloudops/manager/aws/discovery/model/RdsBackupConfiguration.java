package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;

public record RdsBackupConfiguration(
    Integer backupRetentionPeriod,
    String preferredBackupWindow,
    Instant latestRestorableTime,
    Boolean copyTagsToSnapshot
) {}