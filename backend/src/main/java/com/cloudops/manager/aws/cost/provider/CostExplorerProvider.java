package com.cloudops.manager.aws.cost.provider;

import com.cloudops.manager.aws.cost.model.CostAggregationResult;
import com.cloudops.manager.aws.cost.model.CostQueryRequest;
import software.amazon.awssdk.services.costexplorer.CostExplorerClient;

public interface CostExplorerProvider {
    CostAggregationResult getCostAndUsage(CostQueryRequest request, CostExplorerClient client, String billingScope);
}