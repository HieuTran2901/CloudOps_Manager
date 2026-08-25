package com.cloudops.manager.aws.discovery.model;

public record RouteDetail(
    String destinationCidrBlock,
    String destinationIpv6CidrBlock,
    String destinationPrefixListId,
    String targetId,
    String targetType,
    String state
) {}