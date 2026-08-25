package com.cloudops.manager.aws.audit.service;

import com.cloudops.manager.aws.audit.model.CloudTrailEventLookupRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloudTrailValidationUtilsTest {

    @Test
    @DisplayName("Should validate correct CloudTrailEventLookupRequest")
    void shouldValidateCorrectRequest() {
        CloudTrailEventLookupRequest request = new CloudTrailEventLookupRequest(
                "123456789012", "us-east-1", "RunInstances", null, null, null,
                Instant.now().minus(Duration.ofDays(7)), Instant.now(), 50
        );

        assertThatCode(() -> CloudTrailValidationUtils.validateRequest(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject invalid date range where start >= end")
    void shouldRejectInvalidDateRange() {
        CloudTrailEventLookupRequest request = new CloudTrailEventLookupRequest(
                "123456789012", "us-east-1", "RunInstances", null, null, null,
                Instant.now(), Instant.now().minusSeconds(3600), 50
        );

        assertThatThrownBy(() -> CloudTrailValidationUtils.validateRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startTime must be strictly before endTime");
    }

    @Test
    @DisplayName("Should reject query window exceeding 90 days")
    void shouldRejectExcessiveQueryWindow() {
        CloudTrailEventLookupRequest request = new CloudTrailEventLookupRequest(
                "123456789012", "us-east-1", "RunInstances", null, null, null,
                Instant.now().minus(Duration.ofDays(95)), Instant.now(), 50
        );

        assertThatThrownBy(() -> CloudTrailValidationUtils.validateRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 90 days");
    }

    @Test
    @DisplayName("Should reject conflicting lookup attributes (eventName and username)")
    void shouldRejectConflictingAttributes() {
        CloudTrailEventLookupRequest request = new CloudTrailEventLookupRequest(
                "123456789012", "us-east-1", "RunInstances", "alice", null, null,
                Instant.now().minus(Duration.ofDays(1)), Instant.now(), 50
        );

        assertThatThrownBy(() -> CloudTrailValidationUtils.validateRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most one primary lookup attribute");
    }
}