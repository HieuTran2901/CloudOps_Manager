package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.VpcDetailResource;
import com.cloudops.manager.aws.discovery.model.VpcResource;
import com.cloudops.manager.aws.discovery.model.VpcTopologyResource;

import java.util.List;
import java.util.Optional;

public interface VpcProvider {
    List<VpcResource> describeVpcs(String region, String accountId);
    Optional<VpcDetailResource> getVpc(String vpcId, String region, String accountId);
    Optional<VpcTopologyResource> getVpcTopology(String vpcId, String region, String accountId);
}