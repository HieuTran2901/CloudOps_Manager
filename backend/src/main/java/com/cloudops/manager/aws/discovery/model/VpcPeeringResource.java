package com.cloudops.manager.aws.discovery.model;

import java.util.Map;

public record VpcPeeringResource(
    String peeringConnectionId,
    String requesterVpcId,
    String accepterVpcId,
    String requesterCidr,
    String accepterCidr,
    String status,
    String statusMessage,
    Map<String, String> tags
) {}