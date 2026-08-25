package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.Ec2DetailResource;
import com.cloudops.manager.aws.discovery.model.Ec2InstanceResource;

import java.util.List;
import java.util.Optional;

public interface Ec2Provider {
    List<Ec2InstanceResource> describeInstances(String region, String accountId);
    Optional<Ec2DetailResource> getInstance(String instanceId, String region, String accountId);
}