package com.cloudops.manager.aws.discovery.model;

public record RdsParameterGroupInfo(
    String parameterGroupName,
    String parameterApplyStatus
) {}