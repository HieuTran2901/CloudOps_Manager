package com.cloudops.manager.operations.model;

import java.time.Instant;
import java.util.Map;

public record OperationalEvent(
        String eventId,
        Instant timestamp,
        String eventType,
        OperationalSeverity severity,
        String message,
        String sourceSubsystem,
        Map<String, String> sanitizedDetails
) {}