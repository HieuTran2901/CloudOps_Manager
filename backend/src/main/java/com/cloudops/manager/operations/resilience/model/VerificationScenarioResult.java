package com.cloudops.manager.operations.resilience.model;

import java.time.Instant;

public record VerificationScenarioResult(
        String scenarioId,
        String scenarioName,
        String status,
        String simulatedState,
        String observedHandling,
        Instant executedAt,
        boolean isSimulated
) {}