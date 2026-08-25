package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;
import java.util.List;

public record IamIdentityTopologyResource(
    List<IamUserResource> users,
    List<IamRoleResource> roles,
    Instant discoveredAt
) {}