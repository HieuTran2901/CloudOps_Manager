package com.cloudops.manager.aws.topology.service;

import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.aws.topology.extractor.TopologyRelationshipExtractor;
import com.cloudops.manager.aws.topology.model.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

@Component
public class TopologyGraphBuilder {

    private final List<TopologyRelationshipExtractor> extractors;

    public TopologyGraphBuilder(List<TopologyRelationshipExtractor> extractors) {
        this.extractors = (extractors != null) ? List.copyOf(extractors) : List.of();
    }

    public TopologyGraph buildGraph(TopologyContext context) {
        if (context == null) {
            return new TopologyGraph("unknown", "unknown", Instant.now(), 0, 0, List.of(), List.of());
        }

        Map<String, TopologyNode> nodeMap = new HashMap<>();
        String accountId = context.accountId();
        String region = context.region();

        // 1. Construct Nodes
        if (context.ec2Instances() != null) {
            for (Ec2DetailResource ec2 : context.ec2Instances()) {
                TopologyNode node = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, ec2.instanceId(), accountId, region,
                        Map.of("instanceState", ec2.instanceState(), "instanceType", ec2.instanceType() != null ? ec2.instanceType() : ""));
                nodeMap.put(node.nodeId(), node);
            }
        }

        if (context.securityGroups() != null) {
            for (SecurityGroupDetailResource sg : context.securityGroups()) {
                TopologyNode node = TopologyNode.of(TopologyNodeType.SECURITY_GROUP, sg.securityGroupId(), accountId, region,
                        Map.of("groupName", sg.securityGroupName() != null ? sg.securityGroupName() : "", "vpcId", sg.vpcId() != null ? sg.vpcId() : ""));
                nodeMap.put(node.nodeId(), node);
            }
        }

        if (context.vpcTopologies() != null) {
            for (VpcTopologyResource vpcTopo : context.vpcTopologies()) {
                if (vpcTopo.vpc() != null) {
                    TopologyNode vpcNode = TopologyNode.of(TopologyNodeType.VPC, vpcTopo.vpc().vpcId(), accountId, region,
                            Map.of("cidrBlock", vpcTopo.vpc().cidrBlock() != null ? vpcTopo.vpc().cidrBlock() : ""));
                    nodeMap.put(vpcNode.nodeId(), vpcNode);
                }
                if (vpcTopo.subnets() != null) {
                    for (SubnetDetailResource sub : vpcTopo.subnets()) {
                        TopologyNode subNode = TopologyNode.of(TopologyNodeType.SUBNET, sub.subnetId(), accountId, region,
                                Map.of("cidrBlock", sub.cidrBlock() != null ? sub.cidrBlock() : "", "vpcId", sub.vpcId() != null ? sub.vpcId() : ""));
                        nodeMap.put(subNode.nodeId(), subNode);
                    }
                }
            }
        }

        if (context.rdsDatabases() != null) {
            for (RdsDetailResource rds : context.rdsDatabases()) {
                TopologyNode node = TopologyNode.of(TopologyNodeType.RDS_INSTANCE, rds.dbInstanceIdentifier(), accountId, region,
                        Map.of("engine", rds.engine() != null ? rds.engine() : "", "multiAz", rds.multiAz() != null ? rds.multiAz() : false));
                nodeMap.put(node.nodeId(), node);
            }
        }

        if (context.iamRoles() != null) {
            for (IamRoleResource role : context.iamRoles()) {
                TopologyNode node = TopologyNode.of(TopologyNodeType.IAM_ROLE, role.roleName(), accountId, "global",
                        Map.of("arn", role.arn() != null ? role.arn() : ""));
                nodeMap.put(node.nodeId(), node);
            }
        }

        // 2. Extract Edges
        Map<String, TopologyEdge> edgeMap = new HashMap<>();
        for (TopologyRelationshipExtractor extractor : extractors) {
            List<TopologyEdge> candidateEdges = extractor.extract(context);
            if (candidateEdges != null) {
                for (TopologyEdge edge : candidateEdges) {
                    if (nodeMap.containsKey(edge.sourceNodeId()) && nodeMap.containsKey(edge.targetNodeId())) {
                        edgeMap.put(edge.edgeId(), edge);
                    }
                }
            }
        }

        // 3. Deterministic Sorting
        List<TopologyNode> sortedNodes = nodeMap.values().stream().sorted().toList();
        List<TopologyEdge> sortedEdges = edgeMap.values().stream().sorted().toList();

        return new TopologyGraph(
                accountId,
                region,
                Instant.now(),
                sortedNodes.size(),
                sortedEdges.size(),
                sortedNodes,
                sortedEdges
        );
    }
}