package com.cloudops.manager.aws.discovery.model;

import java.util.List;

public record S3CorsRule(
    String id,
    List<String> allowedOrigins,
    List<String> allowedMethods,
    List<String> allowedHeaders,
    List<String> exposeHeaders,
    Integer maxAgeSeconds
) {}