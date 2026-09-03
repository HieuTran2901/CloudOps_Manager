package com.cloudops.manager.operations.impact.model;

public enum ImpactAnalysisStatus {
    SUCCESS,
    NOT_FOUND,
    AMBIGUOUS_RESOURCE,
    INVALID_REQUEST,
    ANALYSIS_LIMIT_REACHED,
    PARTIAL,
    UNSUPPORTED_RESOURCE_TYPE
}
