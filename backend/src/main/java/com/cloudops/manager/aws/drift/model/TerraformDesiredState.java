package com.cloudops.manager.aws.drift.model;

import java.util.List;

public record TerraformDesiredState(
    int formatVersion,
    String terraformVersion,
    long serial,
    List<TerraformDesiredResource> resources
) {
    public TerraformDesiredState {
        resources = (resources != null) ? List.copyOf(resources) : List.of();
    }
}