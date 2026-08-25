package com.cloudops.manager.aws.drift.model;

public record DriftAttributeDifference(
    String attributeName,
    Object desiredValue,
    Object observedValue
) {}