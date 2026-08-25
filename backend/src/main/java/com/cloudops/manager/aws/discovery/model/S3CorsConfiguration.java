package com.cloudops.manager.aws.discovery.model;

import java.util.List;

public record S3CorsConfiguration(
    String status,
    List<S3CorsRule> rules
) {}