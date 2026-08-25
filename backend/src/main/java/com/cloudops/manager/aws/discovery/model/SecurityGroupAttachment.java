package com.cloudops.manager.aws.discovery.model;

import java.util.List;

public record SecurityGroupAttachment(
    List<NetworkInterfaceAttachment> networkInterfaces,
    List<ComputeResourceAttachment> computeInstances,
    List<DatabaseResourceAttachment> databaseInstances
) {}