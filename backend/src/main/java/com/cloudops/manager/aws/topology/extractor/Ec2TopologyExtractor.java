package com.cloudops.manager.aws.topology.extractor;

import com.cloudops.manager.aws.discovery.model.Ec2DetailResource;
import com.cloudops.manager.aws.discovery.model.Ec2NetworkInterfaceDetail;
import com.cloudops.manager.aws.topology.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class Ec2TopologyExtractor implements TopologyRelationshipExtractor {

    @Override
    public List<TopologyEdge> extract(TopologyContext context) {
        if (context == null || context.ec2Instances() == null) {
            return List.of();
        }

        List<TopologyEdge> edges = new ArrayList<>();
        String accountId = context.accountId();
        String region = context.region();

        for (Ec2DetailResource ec2 : context.ec2Instances()) {
            String ec2NodeId = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, ec2.instanceId(), accountId, region, Map.of()).nodeId();

            if (ec2.subnetId() != null && !ec2.subnetId().isBlank()) {
                String subnetNodeId = TopologyNode.of(TopologyNodeType.SUBNET, ec2.subnetId(), accountId, region, Map.of()).nodeId();
                edges.add(TopologyEdge.of(
                        TopologyRelationshipType.EC2_IN_SUBNET,
                        ec2NodeId,
                        subnetNodeId,
                        accountId,
                        region,
                        Map.of("instanceId", ec2.instanceId(), "subnetId", ec2.subnetId())
                ));
            }

            if (ec2.networkInterfaces() != null) {
                for (Ec2NetworkInterfaceDetail eni : ec2.networkInterfaces()) {
                    if (eni.securityGroupIds() != null) {
                        for (String sgId : eni.securityGroupIds()) {
                            String sgNodeId = TopologyNode.of(TopologyNodeType.SECURITY_GROUP, sgId, accountId, region, Map.of()).nodeId();
                            edges.add(TopologyEdge.of(
                                    TopologyRelationshipType.EC2_ATTACHED_SECURITY_GROUP,
                                    ec2NodeId,
                                    sgNodeId,
                                    accountId,
                                    region,
                                    Map.of("instanceId", ec2.instanceId(), "securityGroupId", sgId, "networkInterfaceId", eni.networkInterfaceId())
                            ));
                        }
                    }
                }
            }
        }

        return edges;
    }
}