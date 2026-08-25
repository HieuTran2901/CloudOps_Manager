package com.cloudops.manager.operations.incident.model;

import java.time.Instant;
import java.util.Map;

public record IncidentRecord(
        String incidentId,
        IncidentType type,
        IncidentSeverity severity,
        IncidentStatus status,
        String accountId,
        String region,
        Instant firstDetectedAt,
        Instant lastObservedAt,
        int occurrenceCount,
        String message,
        String source,
        String evidenceState,
        Map<String, String> sanitizedMetadata
) {}