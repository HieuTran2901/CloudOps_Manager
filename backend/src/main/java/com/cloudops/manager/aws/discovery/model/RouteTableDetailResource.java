package com.cloudops.manager.aws.discovery.model;

import java.util.List;
import java.util.Map;

public record RouteTableDetailResource(
    String routeTableId,
    String vpcId,
    Boolean isMain,
    List<String> associatedSubnetIds,
    List<RouteDetail> routes,
    Map<String, String> tags
) {}