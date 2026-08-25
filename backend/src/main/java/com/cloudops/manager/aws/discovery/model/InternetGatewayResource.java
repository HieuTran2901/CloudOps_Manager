package com.cloudops.manager.aws.discovery.model;

import java.util.Map;

public record InternetGatewayResource(
    String internetGatewayId,
    String vpcId,
    String state,
    Map<String, String> tags
) {}