package com.cloudops.manager.aws.discovery.model;

public record RdsOptionGroupInfo(
    String optionGroupName,
    String status
) {}