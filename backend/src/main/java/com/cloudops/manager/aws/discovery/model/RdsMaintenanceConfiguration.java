package com.cloudops.manager.aws.discovery.model;

public record RdsMaintenanceConfiguration(
    String preferredMaintenanceWindow,
    Boolean autoMinorVersionUpgrade,
    Boolean pendingModifiedValues
) {}