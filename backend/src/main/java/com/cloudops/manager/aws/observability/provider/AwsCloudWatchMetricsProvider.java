package com.cloudops.manager.aws.observability.provider;

import com.cloudops.manager.aws.observability.model.MetricDataPoint;
import com.cloudops.manager.aws.observability.model.MetricDataQueryRequest;
import com.cloudops.manager.aws.observability.model.MetricQuery;
import com.cloudops.manager.aws.observability.model.MetricSeries;
import com.cloudops.manager.aws.observability.service.TelemetryProcessingUtils;
import com.cloudops.manager.common.exception.AwsErrorTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;

import java.time.Instant;
import java.util.*;

@Component
public class AwsCloudWatchMetricsProvider implements CloudWatchMetricsProvider {

    private static final Logger log = LoggerFactory.getLogger(AwsCloudWatchMetricsProvider.class);
    private static final int BATCH_SIZE = 50;

    private final CloudWatchClient defaultCloudWatchClient;

    public AwsCloudWatchMetricsProvider(CloudWatchClient defaultCloudWatchClient) {
        this.defaultCloudWatchClient = defaultCloudWatchClient;
    }

    @Override
    public MetricSeries getEc2Metric(MetricQuery query) {
        return querySingleMetric(query, "AWS/EC2", "InstanceId");
    }

    @Override
    public MetricSeries getRdsMetric(MetricQuery query) {
        return querySingleMetric(query, "AWS/RDS", "DBInstanceIdentifier");
    }

    @Override
    public List<MetricSeries> queryMetricData(List<MetricDataQueryRequest> requests, CloudWatchClient targetClient) {
        if (requests == null || requests.isEmpty()) return List.of();
        CloudWatchClient client = targetClient != null ? targetClient : defaultCloudWatchClient;
        List<MetricSeries> results = new ArrayList<>();

        for (int i = 0; i < requests.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, requests.size());
            List<MetricDataQueryRequest> batch = requests.subList(i, end);
            results.addAll(executeBatch(batch, client));
        }
        return results;
    }

    private List<MetricSeries> executeBatch(List<MetricDataQueryRequest> batch, CloudWatchClient client) {
        Map<String, MetricDataQueryRequest> queryMap = new HashMap<>();
        List<MetricDataQuery> queries = new ArrayList<>();
        Instant startTime = batch.get(0).startTime();
        Instant endTime = batch.get(0).endTime();

        for (int idx = 0; idx < batch.size(); idx++) {
            MetricDataQueryRequest req = batch.get(idx);
            String qId = "q_" + idx;
            queryMap.put(qId, req);

            List<Dimension> dims = req.dimensions().entrySet().stream()
                    .map(e -> Dimension.builder().name(e.getKey()).value(e.getValue()).build()).toList();

            MetricStat stat = MetricStat.builder()
                    .metric(Metric.builder().namespace(req.namespace()).metricName(req.metricName()).dimensions(dims).build())
                    .period(req.periodSeconds())
                    .stat(req.statistic())
                    .build();

            queries.add(MetricDataQuery.builder().id(qId).metricStat(stat).returnData(true).build());
        }

        try {
            Map<String, List<MetricDataPoint>> pointsByQueryId = new HashMap<>();
            String nextToken = null;

            do {
                GetMetricDataRequest request = GetMetricDataRequest.builder()
                        .metricDataQueries(queries)
                        .startTime(startTime)
                        .endTime(endTime)
                        .nextToken(nextToken)
                        .build();

                GetMetricDataResponse response = client.getMetricData(request);
                for (MetricDataResult res : response.metricDataResults()) {
                    List<MetricDataPoint> list = pointsByQueryId.computeIfAbsent(res.id(), k -> new ArrayList<>());
                    List<Instant> timestamps = res.timestamps();
                    List<Double> values = res.values();
                    for (int j = 0; j < timestamps.size(); j++) {
                        list.add(new MetricDataPoint(timestamps.get(j), values.get(j), "None", queryMap.get(res.id()).statistic()));
                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null && !nextToken.isBlank());

            List<MetricSeries> batchSeries = new ArrayList<>();
            for (Map.Entry<String, MetricDataQueryRequest> entry : queryMap.entrySet()) {
                String qId = entry.getKey();
                MetricDataQueryRequest req = entry.getValue();
                List<MetricDataPoint> raw = pointsByQueryId.getOrDefault(qId, List.of());
                List<MetricDataPoint> processed = TelemetryProcessingUtils.processDatapoints(raw, req.downsampleFactor(), req.rollupStatistic());

                batchSeries.add(new MetricSeries(
                        req.metricName(), req.resourceId(), req.region(), req.accountId(), "None",
                        req.statistic(), req.periodSeconds(), req.startTime(), req.endTime(), processed
                ));
            }
            return batchSeries;
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("CloudWatch:GetMetricData", e, log);
        }
    }

    private MetricSeries querySingleMetric(MetricQuery query, String namespace, String dimensionName) {
        MetricDataQueryRequest req = new MetricDataQueryRequest(
                namespace, query.metricName(), Map.of(dimensionName, query.instanceId()),
                query.statistic(), query.periodSeconds(), query.startTime(), query.endTime(),
                query.instanceId(), namespace.replace("AWS/", ""), query.accountId(), query.region(), null, null
        );
        List<MetricSeries> result = queryMetricData(List.of(req), defaultCloudWatchClient);
        return result.isEmpty() ? new MetricSeries(query.metricName(), query.instanceId(), query.region(), query.accountId(), "None", query.statistic(), query.periodSeconds(), query.startTime(), query.endTime(), List.of()) : result.get(0);
    }
}