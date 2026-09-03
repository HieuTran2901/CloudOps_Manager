package com.cloudops.manager.operations.risk.model;

import java.util.List;

/**
 * Actionable operational recommendation. Purely instructional (READ/ANALYZE/RECOMMEND).
 */
public record RecommendedAction(
    String actionId,
    String title,
    String description,
    ActionSafety safetyLevel,
    List<String> stepByStepGuide,
    String verificationCheck
) {}
