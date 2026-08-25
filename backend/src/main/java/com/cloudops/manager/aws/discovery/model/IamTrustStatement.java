package com.cloudops.manager.aws.discovery.model;

import java.util.List;
import java.util.Map;

public record IamTrustStatement(
    String effect,
    List<String> principals,
    String action,
    Map<String, Object> conditions
) {}