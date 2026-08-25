package com.cloudops.manager.aws.observability.service;

import com.cloudops.manager.aws.observability.model.MetricDataPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TelemetryProcessingUtils {

    private TelemetryProcessingUtils() {}

    public static List<MetricDataPoint> processDatapoints(List<MetricDataPoint> points, Integer downsampleFactor, String rollupStatistic) {
        if (points == null || points.isEmpty()) {
            return List.of();
        }
        List<MetricDataPoint> sorted = new ArrayList<>(points);
        sorted.sort(Comparator.comparing(MetricDataPoint::timestamp));

        if (downsampleFactor == null || downsampleFactor <= 1 || sorted.size() <= 1) {
            return sorted;
        }

        String stat = (rollupStatistic != null && !rollupStatistic.isBlank()) ? rollupStatistic.trim() : "Average";
        List<MetricDataPoint> downsampled = new ArrayList<>();

        for (int i = 0; i < sorted.size(); i += downsampleFactor) {
            int end = Math.min(i + downsampleFactor, sorted.size());
            List<MetricDataPoint> bucket = sorted.subList(i, end);
            double val = computeRollup(bucket, stat);
            String unit = bucket.get(0).unit();
            downsampled.add(new MetricDataPoint(bucket.get(0).timestamp(), val, unit, stat));
        }

        return downsampled;
    }

    private static double computeRollup(List<MetricDataPoint> bucket, String statistic) {
        return switch (statistic.toUpperCase()) {
            case "SUM" -> bucket.stream().mapToDouble(MetricDataPoint::value).sum();
            case "MINIMUM", "MIN" -> bucket.stream().mapToDouble(MetricDataPoint::value).min().orElse(0.0);
            case "MAXIMUM", "MAX" -> bucket.stream().mapToDouble(MetricDataPoint::value).max().orElse(0.0);
            case "SAMPLECOUNT", "COUNT" -> (double) bucket.size();
            default -> bucket.stream().mapToDouble(MetricDataPoint::value).average().orElse(0.0);
        };
    }
}