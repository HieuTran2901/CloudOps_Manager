package com.cloudops.manager.aws.topology.model;

import com.cloudops.manager.aws.discovery.model.*;

import java.util.List;

public record TopologyContext(
    String accountId,
    String region,
    List<Ec2DetailResource> ec2Instances,
    List<SecurityGroupDetailResource> securityGroups,
    List<VpcTopologyResource> vpcTopologies,
    List<RdsDetailResource> rdsDatabases,
    List<IamRoleResource> iamRoles
) {
    public TopologyContext {
        ec2Instances = (ec2Instances != null) ? List.copyOf(ec2Instances) : List.of();
        securityGroups = (securityGroups != null) ? List.copyOf(securityGroups) : List.of();
        vpcTopologies = (vpcTopologies != null) ? List.copyOf(vpcTopologies) : List.of();
        rdsDatabases = (rdsDatabases != null) ? List.copyOf(rdsDatabases) : List.of();
        iamRoles = (iamRoles != null) ? List.copyOf(iamRoles) : List.of();
    }
}