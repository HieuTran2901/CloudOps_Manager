package com.cloudops.manager.aws.topology.extractor;

import com.cloudops.manager.aws.discovery.model.RdsDetailResource;
import com.cloudops.manager.aws.discovery.model.RdsNetworkConfiguration;
import com.cloudops.manager.aws.topology.model.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RdsTopologyExtractor implements TopologyRelationshipExtractor {

    @Override
    public List<TopologyEdge> extract(TopologyContext context) {
        if (context == null || context.rdsDatabases() == null) {
            return List.of();
        }

        List<TopologyEdge> edges = new ArrayList<>();
        String accountId = context.accountId();
        String region = context.region();

        for (RdsDetailResource rds : context.rdsDatabases()) {
            String rdsNodeId = TopologyNode.of(TopologyNodeType.RDS_INSTANCE, rds.dbInstanceIdentifier(), accountId, region, Map.of()).nodeId();
            RdsNetworkConfiguration net = rds.network();

            if (net != null) {
                if (net.vpcId() != null && !net.vpcId().isBlank()) {
                    String vpcNodeId = TopologyNode.of(TopologyNodeType.VPC, net.vpcId(), accountId, region, Map.of()).nodeId();
                    edges.add(TopologyEdge.of(
                            TopologyRelationshipType.RDS_IN_VPC,
                            rdsNodeId,
                            vpcNodeId,
                            accountId,
                            region,
                            Map.of("dbInstanceIdentifier", rds.dbInstanceIdentifier(), "vpcId", net.vpcId())
                    ));
                }

                if (net.subnetIds() != null) {
                    for (String subnetId : net.subnetIds()) {
                        String subnetNodeId = TopologyNode.of(TopologyNodeType.SUBNET, subnetId, accountId, region, Map.of()).nodeId();
                        edges.add(TopologyEdge.of(
                                TopologyRelationshipType.RDS_IN_SUBNET,
                                rdsNodeId,
                                subnetNodeId,
                                accountId,
                                region,
                                Map.of("dbInstanceIdentifier", rds.dbInstanceIdentifier(), "subnetId", subnetId)
                        ));
                    }
                }

                if (net.securityGroupIds() != null) {
                    for (String sgId : net.securityGroupIds()) {
                        String sgNodeId = TopologyNode.of(TopologyNodeType.SECURITY_GROUP, sgId, accountId, region, Map.of()).nodeId();
                        edges.add(TopologyEdge.of(
                                TopologyRelationshipType.RDS_ATTACHED_SECURITY_GROUP,
                                rdsNodeId,
                                sgNodeId,
                                accountId,
                                region,
                                Map.of("dbInstanceIdentifier", rds.dbInstanceIdentifier(), "securityGroupId", sgId)
                        ));
                    }
                }
            }
        }

        return edges;
    }
}