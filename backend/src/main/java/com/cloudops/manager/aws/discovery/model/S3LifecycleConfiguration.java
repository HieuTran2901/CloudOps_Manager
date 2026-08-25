package com.cloudops.manager.aws.discovery.model;

import java.util.List;

public record S3LifecycleConfiguration(
    String status,
    List<S3LifecycleRule> rules
) {}