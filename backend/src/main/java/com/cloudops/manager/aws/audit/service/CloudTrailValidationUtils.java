package com.cloudops.manager.aws.audit.service;

import com.cloudops.manager.aws.audit.model.CloudTrailEventLookupRequest;

import java.time.Duration;
import java.time.Instant;

public final class CloudTrailValidationUtils {

    private static final Duration MAX_QUERY_WINDOW = Duration.ofDays(90);

    private CloudTrailValidationUtils() {}

    public static void validateRequest(CloudTrailEventLookupRequest request) {
        if (request == null) throw new IllegalArgumentException("CloudTrailEventLookupRequest must not be null");

        Instant start = request.startTime();
        Instant end = request.endTime();
        if (start != null && end != null) {
            if (!start.isBefore(end)) {
                throw new IllegalArgumentException("startTime must be strictly before endTime");
            }
            if (Duration.between(start, end).compareTo(MAX_QUERY_WINDOW) > 0) {
                throw new IllegalArgumentException("CloudTrail lookup query window cannot exceed 90 days");
            }
        }

        if (request.maxResults() != null) {
            if (request.maxResults() < 1 || request.maxResults() > 500) {
                throw new IllegalArgumentException("maxResults must be between 1 and 500");
            }
        }

        int filterCount = 0;
        if (request.eventName() != null && !request.eventName().isBlank()) filterCount++;
        if (request.username() != null && !request.username().isBlank()) filterCount++;
        if (request.resourceName() != null && !request.resourceName().isBlank()) filterCount++;
        if (request.resourceType() != null && !request.resourceType().isBlank()) filterCount++;

        if (filterCount > 1) {
            throw new IllegalArgumentException("CloudTrail LookupEvents supports at most one primary lookup attribute (eventName, username, resourceName, or resourceType)");
        }
    }
}