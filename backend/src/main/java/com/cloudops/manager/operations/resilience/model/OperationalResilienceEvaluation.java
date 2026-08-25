package com.cloudops.manager.operations.resilience.model;

import com.cloudops.manager.operations.evidence.model.EvidenceLifecycleRecord;
import com.cloudops.manager.operations.incident.model.IncidentRecord;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OperationalResilienceEvaluation(
        String overallScore,
        boolean isResilient,
        Map<String, String> dimensionStates,
        List<IncidentRecord> activeIncidents,
        List<EvidenceLifecycleRecord> evidenceStates,
        String accountId,
        String region,
        String canonicalDigest,
        Instant evaluatedAt,
        String summary
) {}