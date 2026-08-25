package com.cloudops.manager.aws.cost.provider;

import com.cloudops.manager.aws.cost.model.*;
import com.cloudops.manager.common.exception.AwsErrorTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;
import software.amazon.awssdk.services.costexplorer.model.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Component
public class AwsCostExplorerProvider implements CostExplorerProvider {

    private static final Logger log = LoggerFactory.getLogger(AwsCostExplorerProvider.class);
    private final CostExplorerClient defaultCostExplorerClient;

    public AwsCostExplorerProvider(CostExplorerClient defaultCostExplorerClient) {
        this.defaultCostExplorerClient = defaultCostExplorerClient;
    }

    @Override
    public CostAggregationResult getCostAndUsage(CostQueryRequest request, CostExplorerClient targetClient, String billingScope) {
        log.info("Executing Cost Explorer GetCostAndUsage for account: {}, metric: {}, granularity: {}",
                request.accountId(), request.metric(), request.granularity());

        CostExplorerClient client = targetClient != null ? targetClient : defaultCostExplorerClient;

        try {
            DateInterval interval = DateInterval.builder()
                    .start(request.startDate().toString())
                    .end(request.endDate().toString())
                    .build();

            List<GroupDefinition> groupDefs = new ArrayList<>();
            if (request.groupByDimensions() != null) {
                for (String dim : request.groupByDimensions()) {
                    groupDefs.add(GroupDefinition.builder().type(GroupDefinitionType.DIMENSION).key(dim).build());
                }
            }

            List<CostPeriodResult> periodResults = new ArrayList<>();
            BigDecimal grandTotal = BigDecimal.ZERO;
            String unit = "USD";
            String nextPageToken = null;

            do {
                GetCostAndUsageRequest.Builder reqBuilder = GetCostAndUsageRequest.builder()
                        .timePeriod(interval)
                        .granularity(Granularity.fromValue(request.granularity().toUpperCase()))
                        .metrics(request.metric())
                        .nextPageToken(nextPageToken);

                if (!groupDefs.isEmpty()) {
                    reqBuilder.groupBy(groupDefs);
                }

                GetCostAndUsageResponse response = client.getCostAndUsage(reqBuilder.build());

                for (ResultByTime rbt : response.resultsByTime()) {
                    CostTimePeriod timePeriod = new CostTimePeriod(rbt.timePeriod().start(), rbt.timePeriod().end());
                    BigDecimal periodTotal = BigDecimal.ZERO;
                    List<CostGroup> groups = new ArrayList<>();

                    if (rbt.hasTotal() && rbt.total().containsKey(request.metric())) {
                        MetricValue mv = rbt.total().get(request.metric());
                        periodTotal = new BigDecimal(mv.amount());
                        if (mv.unit() != null) unit = mv.unit();
                    }

                    if (rbt.hasGroups() && !rbt.groups().isEmpty()) {
                        for (Group g : rbt.groups()) {
                            BigDecimal groupAmount = BigDecimal.ZERO;
                            String groupUnit = unit;
                            if (g.metrics() != null && g.metrics().containsKey(request.metric())) {
                                MetricValue gmv = g.metrics().get(request.metric());
                                groupAmount = new BigDecimal(gmv.amount());
                                if (gmv.unit() != null) groupUnit = gmv.unit();
                            }
                            groups.add(new CostGroup(g.keys(), groupAmount, groupUnit));
                        }
                    }

                    grandTotal = grandTotal.add(periodTotal);
                    periodResults.add(new CostPeriodResult(timePeriod, periodTotal, unit, groups));
                }

                nextPageToken = response.nextPageToken();
            } while (nextPageToken != null && !nextPageToken.isBlank());

            CostTimePeriod overallPeriod = new CostTimePeriod(request.startDate().toString(), request.endDate().toString());
            return new CostAggregationResult(
                    request.accountId(),
                    billingScope != null ? billingScope : "STANDALONE_ACCOUNT",
                    request.metric(),
                    request.granularity().toUpperCase(),
                    overallPeriod,
                    grandTotal,
                    unit,
                    periodResults,
                    Instant.now()
            );
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("CostExplorer:GetCostAndUsage", e, log);
        }
    }
}