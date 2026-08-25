package com.cloudops.manager.aws.topology.extractor;

import com.cloudops.manager.aws.discovery.model.SubnetDetailResource;
import com.cloudops.manager.aws.discovery.model.VpcTopologyResource;
import com.cloudops.manager.aws.topology.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SubnetVpcTopologyExtractor implements TopologyRelationshipExtractor {

    @Override
    public List<TopologyEdge> extract(TopologyContext context) {
        if (context == null || context.vpcTopologies() == null) {
            return List.of();
        }

        List<TopologyEdge> edges = new ArrayList<>();
        String accountId = context.accountId();
        String region = context.region();

        for (VpcTopologyResource vpcTopo : context.vpcTopologies()) {
            if (vpcTopo.vpc() == null) continue;
            String vpcId = vpcTopo.vpc().vpcId();
            String vpcNodeId = TopologyNode.of(TopologyNodeType.VPC, vpcId, accountId, region, Map.of()).nodeId();

            if (vpcTopo.subnets() != null) {
                for (SubnetDetailResource subnet : vpcTopo.subnets()) {
                    String subnetNodeId = TopologyNode.of(TopologyNodeType.SUBNET, subnet.subnetId(), accountId, region, Map.of()).nodeId();
                    edges.add(TopologyEdge.of(
                            TopologyRelationshipType.SUBNET_IN_VPC,
                            subnetNodeId,
                            vpcNodeId,
                            accountId,
                            region,
                            Map.of("subnetId", subnet.subnetId(), "vpcId", vpcId)
                    ));
                }
            }
        }

        return edges;
    }
}