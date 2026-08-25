package com.cloudops.manager.aws.discovery.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.Map;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "resourceType", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Ec2InstanceResource.class, name = "EC2_INSTANCE"),
    @JsonSubTypes.Type(value = S3BucketResource.class, name = "S3_BUCKET"),
    @JsonSubTypes.Type(value = RdsInstanceResource.class, name = "RDS_INSTANCE"),
    @JsonSubTypes.Type(value = VpcResource.class, name = "VPC"),
    @JsonSubTypes.Type(value = SecurityGroupResource.class, name = "SECURITY_GROUP")
})
public interface CloudResource {
    String resourceId();
    CloudResourceType resourceType();
    String name();
    String region();
    String accountId();
    String status();
    String arn();
    Map<String, String> tags();
    Instant discoveredAt();
}