package com.cloudops.manager.aws.discovery.model;

import java.util.List;
import java.util.Map;

public record IamPolicyStatement(
    String effect,
    List<String> actions,
    List<String> notActions,
    List<String> resources,
    List<String> notResources,
    Map<String, Object> condition
) {}