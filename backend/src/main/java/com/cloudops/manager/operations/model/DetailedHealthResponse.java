package com.cloudops.manager.operations.model;

import java.time.Instant;
import java.util.Map;

public record DetailedHealthResponse(
        String status,
        String service,
        String version,
        String release,
        Map<String, HealthStatus> components,
        Instant timestamp
) {}