package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;

public record SecurityGroupTopologyResource(
    SecurityGroupDetailResource securityGroup,
    SecurityGroupAttachment attachments,
    Instant discoveredAt
) {}