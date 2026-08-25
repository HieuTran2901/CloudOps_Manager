package com.cloudops.manager.aws.observability.service;

import com.cloudops.manager.aws.observability.model.MetricDataPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TelemetryProcessingUtilsTest {

    @Test
    @DisplayName("Should sort datapoints chronologically")
    void shouldSortDatapoints() {
        Instant t1 = Instant.parse("2026-08-23T10:00:00Z");
        Instant t2 = Instant.parse("2026-08-23T10:05:00Z");
        Instant t3 = Instant.parse("2026-08-23T10:10:00Z");

        List<MetricDataPoint> points = List.of(
                new MetricDataPoint(t2, 20.0, "Percent", "Average"),
                new MetricDataPoint(t3, 30.0, "Percent", "Average"),
                new MetricDataPoint(t1, 10.0, "Percent", "Average")
        );

        List<MetricDataPoint> processed = TelemetryProcessingUtils.processDatapoints(points, 1, "Average");

        assertThat(processed).hasSize(3);
        assertThat(processed.get(0).timestamp()).isEqualTo(t1);
        assertThat(processed.get(1).timestamp()).isEqualTo(t2);
        assertThat(processed.get(2).timestamp()).isEqualTo(t3);
    }

    @Test
    @DisplayName("Should downsample datapoints using Average rollup")
    void shouldDownsampleWithAverage() {
        Instant t1 = Instant.parse("2026-08-23T10:00:00Z");
        Instant t2 = Instant.parse("2026-08-23T10:05:00Z");
        Instant t3 = Instant.parse("2026-08-23T10:10:00Z");
        Instant t4 = Instant.parse("2026-08-23T10:15:00Z");

        List<MetricDataPoint> points = List.of(
                new MetricDataPoint(t1, 10.0, "Percent", "Average"),
                new MetricDataPoint(t2, 20.0, "Percent", "Average"),
                new MetricDataPoint(t3, 30.0, "Percent", "Average"),
                new MetricDataPoint(t4, 40.0, "Percent", "Average")
        );

        List<MetricDataPoint> downsampled = TelemetryProcessingUtils.processDatapoints(points, 2, "Average");

        assertThat(downsampled).hasSize(2);
        assertThat(downsampled.get(0).value()).isEqualTo(15.0); // (10 + 20) / 2
        assertThat(downsampled.get(1).value()).isEqualTo(35.0); // (30 + 40) / 2
    }

    @Test
    @DisplayName("Should downsample datapoints using Sum rollup")
    void shouldDownsampleWithSum() {
        Instant t1 = Instant.parse("2026-08-23T10:00:00Z");
        Instant t2 = Instant.parse("2026-08-23T10:05:00Z");

        List<MetricDataPoint> points = List.of(
                new MetricDataPoint(t1, 100.0, "Bytes", "Sum"),
                new MetricDataPoint(t2, 200.0, "Bytes", "Sum")
        );

        List<MetricDataPoint> downsampled = TelemetryProcessingUtils.processDatapoints(points, 2, "Sum");

        assertThat(downsampled).hasSize(1);
        assertThat(downsampled.get(0).value()).isEqualTo(300.0);
    }
}