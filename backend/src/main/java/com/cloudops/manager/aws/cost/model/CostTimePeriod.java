package com.cloudops.manager.aws.cost.model;

public record CostTimePeriod(
    String start,
    String end
) {}