package com.cloudops.manager.aws.drift.service;

import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.aws.drift.model.DriftAttributeDifference;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TerraformResourceNormalizer {

    public List<DriftAttributeDifference> compareEc2(Map<String, Object> desired, Ec2DetailResource observed) {
        List<DriftAttributeDifference> diffs = new ArrayList<>();

        if (desired.containsKey("instance_type")) {
            String desiredType = String.valueOf(desired.get("instance_type"));
            if (!desiredType.equalsIgnoreCase(observed.instanceType())) {
                diffs.add(new DriftAttributeDifference("instance_type", desiredType, observed.instanceType()));
            }
        }

        if (desired.containsKey("subnet_id")) {
            String desiredSubnet = String.valueOf(desired.get("subnet_id"));
            if (!desiredSubnet.equalsIgnoreCase(observed.subnetId())) {
                diffs.add(new DriftAttributeDifference("subnet_id", desiredSubnet, observed.subnetId()));
            }
        }

        return diffs;
    }

    public List<DriftAttributeDifference> compareSecurityGroup(Map<String, Object> desired, SecurityGroupDetailResource observed) {
        List<DriftAttributeDifference> diffs = new ArrayList<>();

        if (desired.containsKey("vpc_id")) {
            String desiredVpc = String.valueOf(desired.get("vpc_id"));
            if (!desiredVpc.equalsIgnoreCase(observed.vpcId())) {
                diffs.add(new DriftAttributeDifference("vpc_id", desiredVpc, observed.vpcId()));
            }
        }

        if (desired.containsKey("name")) {
            String desiredName = String.valueOf(desired.get("name"));
            if (!desiredName.equalsIgnoreCase(observed.securityGroupName())) {
                diffs.add(new DriftAttributeDifference("name", desiredName, observed.securityGroupName()));
            }
        }

        return diffs;
    }

    public List<DriftAttributeDifference> compareRds(Map<String, Object> desired, RdsDetailResource observed) {
        List<DriftAttributeDifference> diffs = new ArrayList<>();

        if (desired.containsKey("instance_class")) {
            String desiredClass = String.valueOf(desired.get("instance_class"));
            if (!desiredClass.equalsIgnoreCase(observed.dbInstanceClass())) {
                diffs.add(new DriftAttributeDifference("instance_class", desiredClass, observed.dbInstanceClass()));
            }
        }

        if (desired.containsKey("multi_az")) {
            Boolean desiredMultiAz = Boolean.valueOf(String.valueOf(desired.get("multi_az")));
            if (!Objects.equals(desiredMultiAz, observed.multiAz())) {
                diffs.add(new DriftAttributeDifference("multi_az", desiredMultiAz, observed.multiAz()));
            }
        }

        return diffs;
    }

    public List<DriftAttributeDifference> compareS3(Map<String, Object> desired, S3DetailResource observed) {
        List<DriftAttributeDifference> diffs = new ArrayList<>();

        if (desired.containsKey("bucket")) {
            String desiredBucket = String.valueOf(desired.get("bucket"));
            if (!desiredBucket.equalsIgnoreCase(observed.bucketName())) {
                diffs.add(new DriftAttributeDifference("bucket", desiredBucket, observed.bucketName()));
            }
        }

        return diffs;
    }

    public List<DriftAttributeDifference> compareVpc(Map<String, Object> desired, VpcDetailResource observed) {
        List<DriftAttributeDifference> diffs = new ArrayList<>();

        if (desired.containsKey("cidr_block")) {
            String desiredCidr = String.valueOf(desired.get("cidr_block"));
            if (!desiredCidr.equalsIgnoreCase(observed.cidrBlock())) {
                diffs.add(new DriftAttributeDifference("cidr_block", desiredCidr, observed.cidrBlock()));
            }
        }

        return diffs;
    }
}