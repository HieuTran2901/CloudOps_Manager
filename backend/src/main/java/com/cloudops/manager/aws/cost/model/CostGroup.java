package com.cloudops.manager.aws.cost.model;

import java.math.BigDecimal;
import java.util.List;

public record CostGroup(
    List<String> keys,
    BigDecimal amount,
    String unit
) {}