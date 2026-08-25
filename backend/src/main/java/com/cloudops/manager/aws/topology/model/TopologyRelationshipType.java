package com.cloudops.manager.aws.topology.model;

public enum TopologyRelationshipType {
    EC2_IN_SUBNET,
    SUBNET_IN_VPC,
    EC2_ATTACHED_SECURITY_GROUP,
    RDS_IN_VPC,
    RDS_IN_SUBNET,
    RDS_ATTACHED_SECURITY_GROUP,
    EC2_USES_IAM_ROLE
}