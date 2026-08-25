package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.RdsDetailResource;
import com.cloudops.manager.aws.discovery.model.RdsInstanceResource;

import java.util.List;
import java.util.Optional;

public interface RdsProvider {
    List<RdsInstanceResource> describeDbInstances(String region, String accountId);
    Optional<RdsDetailResource> getDbInstance(String dbInstanceIdentifier, String region, String accountId);
}