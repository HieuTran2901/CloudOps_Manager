package com.cloudops.manager.aws.topology.service;

import com.cloudops.manager.aws.discovery.model.Ec2DetailResource;
import com.cloudops.manager.aws.discovery.model.SubnetDetailResource;
import com.cloudops.manager.aws.discovery.model.VpcDetailResource;
import com.cloudops.manager.aws.discovery.model.VpcTopologyResource;
import com.cloudops.manager.aws.topology.extractor.Ec2TopologyExtractor;
import com.cloudops.manager.aws.topology.extractor.SubnetVpcTopologyExtractor;
import com.cloudops.manager.aws.topology.model.TopologyContext;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TopologyDeterminismTest {

    @Test
    @DisplayName("Evaluating topology graph repeatedly on identical context must produce identical node and edge ordering")
    void shouldProduceDeterministicGraph() {
        TopologyGraphBuilder builder = new TopologyGraphBuilder(List.of(new Ec2TopologyExtractor(), new SubnetVpcTopologyExtractor()));

        VpcDetailResource vpc = new VpcDetailResource("vpc-1", "arn", "123", "us-east-1", "available", "10.0.0.0/16", List.of(), List.of(), "dopt-1", "default", false, true, true, Map.of(), Instant.now());
        SubnetDetailResource sub = new SubnetDetailResource("sub-1", "arn", "vpc-1", "10.0.1.0/24", null, "us-east-1a", "use1-az1", "available", false, false, 250, true, Map.of());
        VpcTopologyResource vpcTopo = new VpcTopologyResource(vpc, List.of(sub), List.of(), List.of(), List.of(), List.of(), List.of(), Instant.now());

        Ec2DetailResource ec2 = new Ec2DetailResource(
                "i-1", "web", "arn", "123", "us-east-1", "t3.micro", "x86", "Linux", "Linux",
                "ami-1", null, Instant.now(), "running", null, null, "disabled", "us-east-1a", null,
                "default", "vpc-1", "sub-1", "10.0.0.1", null, "dns", "dns", List.of(), List.of(), Map.of(), Instant.now()
        );

        TopologyContext ctx = new TopologyContext("123", "us-east-1", List.of(ec2), List.of(), List.of(vpcTopo), List.of(), List.of());

        TopologyGraph g1 = builder.buildGraph(ctx);
        TopologyGraph g2 = builder.buildGraph(ctx);

        assertThat(g1.nodeCount()).isEqualTo(g2.nodeCount());
        assertThat(g1.edgeCount()).isEqualTo(g2.edgeCount());
        for (int i = 0; i < g1.nodeCount(); i++) {
            assertThat(g1.nodes().get(i).nodeId()).isEqualTo(g2.nodes().get(i).nodeId());
        }
        for (int i = 0; i < g1.edgeCount(); i++) {
            assertThat(g1.edges().get(i).edgeId()).isEqualTo(g2.edges().get(i).edgeId());
        }
    }
}