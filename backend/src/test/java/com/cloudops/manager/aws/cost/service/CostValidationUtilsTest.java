package com.cloudops.manager.aws.cost.service;

import com.cloudops.manager.aws.cost.model.CostQueryRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CostValidationUtilsTest {

    @Test
    @DisplayName("Should validate correct CostQueryRequest")
    void shouldValidateCorrectRequest() {
        CostQueryRequest request = new CostQueryRequest(
                "123456789012",
                "UnblendedCost",
                "MONTHLY",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 1),
                List.of("SERVICE"),
                null
        );

        assertThatCode(() -> CostValidationUtils.validateRequest(request)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should reject unsupported metric")
    void shouldRejectUnsupportedMetric() {
        CostQueryRequest request = new CostQueryRequest(
                "123456789012",
                "EstimatedCost",
                "MONTHLY",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 1),
                null,
                null
        );

        assertThatThrownBy(() -> CostValidationUtils.validateRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid metric");
    }

    @Test
    @DisplayName("Should reject invalid date range where start >= end")
    void shouldRejectInvalidDateRange() {
        CostQueryRequest request = new CostQueryRequest(
                "123456789012",
                "UnblendedCost",
                "MONTHLY",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 4, 1),
                null,
                null
        );

        assertThatThrownBy(() -> CostValidationUtils.validateRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startDate must be strictly before endDate");
    }

    @Test
    @DisplayName("Should reject more than 2 GroupBy dimensions")
    void shouldRejectExcessiveGroupBy() {
        CostQueryRequest request = new CostQueryRequest(
                "123456789012",
                "UnblendedCost",
                "MONTHLY",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 1),
                List.of("SERVICE", "LINKED_ACCOUNT", "USAGE_TYPE"),
                null
        );

        assertThatThrownBy(() -> CostValidationUtils.validateRequest(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most 2 GroupBy dimensions");
    }
}