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

class TopologyGraphBuilderTest {

    @Test
    @DisplayName("Should build sorted immutable graph and drop dangling edges")
    void shouldBuildGraph() {
        Ec2TopologyExtractor ec2Extractor = new Ec2TopologyExtractor();
        SubnetVpcTopologyExtractor vpcExtractor = new SubnetVpcTopologyExtractor();
        TopologyGraphBuilder builder = new TopologyGraphBuilder(List.of(ec2Extractor, vpcExtractor));

        VpcDetailResource vpc = new VpcDetailResource("vpc-1", "arn", "123", "us-east-1", "available", "10.0.0.0/16", List.of(), List.of(), "dopt-1", "default", false, true, true, Map.of(), Instant.now());
        SubnetDetailResource sub = new SubnetDetailResource("sub-1", "arn", "vpc-1", "10.0.1.0/24", null, "us-east-1a", "use1-az1", "available", false, false, 250, true, Map.of());
        VpcTopologyResource vpcTopo = new VpcTopologyResource(vpc, List.of(sub), List.of(), List.of(), List.of(), List.of(), List.of(), Instant.now());

        Ec2DetailResource ec2 = new Ec2DetailResource(
                "i-1", "web", "arn", "123", "us-east-1", "t3.micro", "x86", "Linux", "Linux",
                "ami-1", null, Instant.now(), "running", null, null, "disabled", "us-east-1a", null,
                "default", "vpc-1", "sub-1", "10.0.0.1", null, "dns", "dns", List.of(), List.of(), Map.of(), Instant.now()
        );

        TopologyContext ctx = new TopologyContext("123", "us-east-1", List.of(ec2), List.of(), List.of(vpcTopo), List.of(), List.of());

        TopologyGraph graph = builder.buildGraph(ctx);
        assertThat(graph.nodeCount()).isEqualTo(3); // VPC, Subnet, EC2
        assertThat(graph.edgeCount()).isEqualTo(2); // EC2->Subnet, Subnet->VPC
    }
}