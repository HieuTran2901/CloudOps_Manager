package com.cloudops.manager.operations.incident.service;

import com.cloudops.manager.operations.incident.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class IncidentManagementService {

    private static final int MAX_INCIDENTS = 100;
    private final Map<String, IncidentRecord> incidentStore = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1000);

    public IncidentRecord recordOrUpdateIncident(
            IncidentType type,
            IncidentSeverity severity,
            String accountId,
            String region,
            String message,
            String source,
            String evidenceState,
            Map<String, String> metadata) {

        String correlationKey = buildCorrelationKey(type, accountId, region, source);
        Instant now = Instant.now();

        return incidentStore.compute(correlationKey, (key, existing) -> {
            if (existing == null) {
                if (incidentStore.size() >= MAX_INCIDENTS) {
                    pruneOldestResolved();
                }
                String incidentId = "INC-" + idSequence.incrementAndGet();
                return new IncidentRecord(
                        incidentId,
                        type,
                        severity,
                        IncidentStatus.OPEN,
                        accountId != null ? accountId : "GLOBAL",
                        region != null ? region : "GLOBAL",
                        now,
                        now,
                        1,
                        message,
                        source,
                        evidenceState,
                        metadata != null ? metadata : Map.of()
                );
            } else {
                IncidentStatus nextStatus = (existing.status() == IncidentStatus.RESOLVED)
                        ? IncidentStatus.OPEN
                        : existing.status();
                return new IncidentRecord(
                        existing.incidentId(),
                        existing.type(),
                        severity,
                        nextStatus,
                        existing.accountId(),
                        existing.region(),
                        existing.firstDetectedAt(),
                        now,
                        existing.occurrenceCount() + 1,
                        message,
                        existing.source(),
                        evidenceState,
                        metadata != null ? metadata : existing.sanitizedMetadata()
                );
            }
        });
    }

    public Optional<IncidentRecord> markRecovering(String incidentId) {
        return updateIncidentStatus(incidentId, IncidentStatus.RECOVERING);
    }

    public Optional<IncidentRecord> resolveIncident(String incidentId) {
        return updateIncidentStatus(incidentId, IncidentStatus.RESOLVED);
    }

    private Optional<IncidentRecord> updateIncidentStatus(String incidentId, IncidentStatus newStatus) {
        for (Map.Entry<String, IncidentRecord> entry : incidentStore.entrySet()) {
            if (entry.getValue().incidentId().equals(incidentId)) {
                IncidentRecord updated = new IncidentRecord(
                        entry.getValue().incidentId(),
                        entry.getValue().type(),
                        entry.getValue().severity(),
                        newStatus,
                        entry.getValue().accountId(),
                        entry.getValue().region(),
                        entry.getValue().firstDetectedAt(),
                        Instant.now(),
                        entry.getValue().occurrenceCount(),
                        entry.getValue().message(),
                        entry.getValue().source(),
                        entry.getValue().evidenceState(),
                        entry.getValue().sanitizedMetadata()
                );
                incidentStore.put(entry.getKey(), updated);
                return Optional.of(updated);
            }
        }
        return Optional.empty();
    }

    public List<IncidentRecord> getActiveIncidents() {
        return incidentStore.values().stream()
                .filter(i -> i.status() != IncidentStatus.RESOLVED)
                .sorted(Comparator.comparing(IncidentRecord::lastObservedAt).reversed())
                .toList();
    }

    public List<IncidentRecord> getAllIncidents() {
        return incidentStore.values().stream()
                .sorted(Comparator.comparing(IncidentRecord::lastObservedAt).reversed())
                .toList();
    }

    private String buildCorrelationKey(IncidentType type, String accountId, String region, String source) {
        return String.format("%s:%s:%s:%s",
                type != null ? type.name() : "UNKNOWN",
                accountId != null ? accountId : "GLOBAL",
                region != null ? region : "GLOBAL",
                source != null ? source : "UNKNOWN"
        );
    }

    private void pruneOldestResolved() {
        incidentStore.entrySet().stream()
                .filter(e -> e.getValue().status() == IncidentStatus.RESOLVED)
                .min(Comparator.comparing(e -> e.getValue().lastObservedAt()))
                .ifPresent(e -> incidentStore.remove(e.getKey()));
    }
}