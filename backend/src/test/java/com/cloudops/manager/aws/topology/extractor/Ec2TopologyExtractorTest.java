package com.cloudops.manager.aws.topology.extractor;

import com.cloudops.manager.aws.discovery.model.Ec2DetailResource;
import com.cloudops.manager.aws.discovery.model.Ec2NetworkInterfaceDetail;
import com.cloudops.manager.aws.topology.model.TopologyContext;
import com.cloudops.manager.aws.topology.model.TopologyEdge;
import com.cloudops.manager.aws.topology.model.TopologyRelationshipType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class Ec2TopologyExtractorTest {

    private final Ec2TopologyExtractor extractor = new Ec2TopologyExtractor();

    @Test
    @DisplayName("Should extract EC2_IN_SUBNET and EC2_ATTACHED_SECURITY_GROUP edges")
    void shouldExtractEc2Relationships() {
        Ec2NetworkInterfaceDetail eni = new Ec2NetworkInterfaceDetail(
                "eni-1", "sub-1", "vpc-1", "10.0.0.1", List.of("10.0.0.1"), "54.200.1.1",
                "00:11", List.of("sg-1"), List.of("web-sg"), "in-use", "", "interface"
        );

        Ec2DetailResource ec2 = new Ec2DetailResource(
                "i-1", "web", "arn", "123456789012", "us-east-1", "t3.micro", "x86", "Linux", "Linux",
                "ami-1", null, Instant.now(), "running", null, null, "disabled", "us-east-1a", null,
                "default", "vpc-1", "sub-1", "10.0.0.1", "54.200.1.1", "dns", "dns", List.of(), List.of(eni), Map.of(), Instant.now()
        );

        TopologyContext ctx = new TopologyContext(
                "123456789012", "us-east-1", List.of(ec2), List.of(), List.of(), List.of(), List.of()
        );

        List<TopologyEdge> edges = extractor.extract(ctx);
        assertThat(edges).hasSize(2);
        assertThat(edges).anyMatch(e -> e.relationshipType() == TopologyRelationshipType.EC2_IN_SUBNET);
        assertThat(edges).anyMatch(e -> e.relationshipType() == TopologyRelationshipType.EC2_ATTACHED_SECURITY_GROUP);
    }
}