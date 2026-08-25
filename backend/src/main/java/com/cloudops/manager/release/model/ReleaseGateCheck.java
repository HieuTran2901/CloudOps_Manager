package com.cloudops.manager.release.model;

public record ReleaseGateCheck(
        String category,
        String name,
        ReleaseGateStatus status,
        ReleaseGateSeverity severity,
        String message,
        String evidenceDetails
) {}