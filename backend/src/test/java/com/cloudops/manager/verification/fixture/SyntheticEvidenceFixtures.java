package com.cloudops.manager.verification.fixture;

import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.aws.forensics.model.ForensicEvidenceItem;
import com.cloudops.manager.aws.forensics.model.ForensicExportFormat;
import com.cloudops.manager.aws.forensics.model.ForensicMetadata;
import com.cloudops.manager.aws.security.model.ExposureStatus;
import com.cloudops.manager.aws.security.model.SecurityExposureResult;
import com.cloudops.manager.aws.topology.model.TopologyEdge;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.model.TopologyNode;
import com.cloudops.manager.aws.topology.model.TopologyNodeType;
import com.cloudops.manager.aws.topology.model.TopologyRelationshipType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SyntheticEvidenceFixtures {

    public static final String SYNTHETIC_ACCOUNT_A = "synthetic-account-a-111122223333";
    public static final String SYNTHETIC_ACCOUNT_B = "synthetic-account-b-444455556666";
    public static final String REGION_US_EAST_1 = "us-east-1";
    public static final String REGION_EU_WEST_1 = "eu-west-1";
    public static final String REGION_AP_SOUTHEAST_2 = "ap-southeast-2";

    private SyntheticEvidenceFixtures() {}

    public static CloudResource createSyntheticResource(String accountId, String region, String id) {
        return new Ec2InstanceResource(
                "synthetic-" + id,
                CloudResourceType.EC2_INSTANCE,
                "synthetic-name-" + id,
                region,
                accountId,
                "RUNNING",
                "arn:aws:ec2:" + region + ":" + accountId + ":instance/synthetic-" + id,
                Map.of("syntheticTag", "phase22-verification", "accountId", accountId),
                Instant.now(),
                "t3.micro",
                "10.0.0.1",
                "54.0.0.1",
                "vpc-01",
                "sub-01",
                region + "a",
                "ami-12345",
                Instant.now()
        );
    }

    public static InventorySummary createSyntheticInventory(String accountId, String region, int count) {
        List<CloudResource> list = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            list.add(createSyntheticResource(accountId, region, "i-" + String.format("%04d", i)));
        }
        return new InventorySummary(
                accountId,
                region,
                list.size(),
                Map.of(CloudResourceType.EC2_INSTANCE, list.size()),
                list,
                Instant.now()
        );
    }

    public static TopologyNode createSyntheticNode(String accountId, String region, TopologyNodeType type, String resId) {
        return TopologyNode.of(type, "synthetic-" + resId, accountId, region, Map.of("accountId", accountId));
    }

    public static TopologyEdge createSyntheticEdge(TopologyNode src, TopologyNode dst, TopologyRelationshipType type, String accountId, String region) {
        return TopologyEdge.of(type, src.nodeId(), dst.nodeId(), accountId, region, Map.of("syntheticEdge", "verified"));
    }

    public static TopologyGraph createSyntheticGraph(String accountId, String region) {
        TopologyNode vpc = createSyntheticNode(accountId, region, TopologyNodeType.VPC, "vpc-001");
        TopologyNode sub = createSyntheticNode(accountId, region, TopologyNodeType.SUBNET, "sub-001");
        TopologyNode ec2 = createSyntheticNode(accountId, region, TopologyNodeType.EC2_INSTANCE, "i-001");
        TopologyNode rds = createSyntheticNode(accountId, region, TopologyNodeType.RDS_INSTANCE, "rds-001");

        TopologyEdge e1 = createSyntheticEdge(sub, vpc, TopologyRelationshipType.SUBNET_IN_VPC, accountId, region);
        TopologyEdge e2 = createSyntheticEdge(ec2, sub, TopologyRelationshipType.EC2_IN_SUBNET, accountId, region);
        TopologyEdge e3 = createSyntheticEdge(rds, sub, TopologyRelationshipType.RDS_IN_SUBNET, accountId, region);

        return new TopologyGraph(
                accountId,
                region,
                Instant.now(),
                4,
                3,
                List.of(vpc, sub, ec2, rds),
                List.of(e1, e2, e3)
        );
    }

    public static SecurityExposureResult createSyntheticExposure(TopologyNode node) {
        return new SecurityExposureResult(
                node.nodeId(),
                node.resourceId(),
                node.resourceType().name(),
                ExposureStatus.EXPOSED,
                Map.of("syntheticFinding", "Port 22 open"),
                node.accountId(),
                node.region()
        );
    }

    public static ForensicEvidenceItem createSyntheticEvidenceItem(String accountId, String region, String subsystem, String id) {
        return new ForensicEvidenceItem(
                subsystem,
                "SYNTHETIC_RESOURCE",
                "synthetic-res-" + id,
                accountId,
                region,
                subsystem,
                Map.of("verificationStatus", "SYNTHETIC_VERIFIED", "key", id)
        );
    }

    public static ForensicMetadata createSyntheticMetadata(String accountId, String region, int count, String sha256) {
        return new ForensicMetadata(
                "synthetic-manifest-001",
                accountId,
                region,
                Instant.parse("2026-08-24T00:00:00Z"),
                ForensicExportFormat.JSON,
                sha256,
                count,
                Map.of("DISCOVERY", count)
        );
    }
}