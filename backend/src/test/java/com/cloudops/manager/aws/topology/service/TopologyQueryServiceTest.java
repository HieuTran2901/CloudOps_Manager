package com.cloudops.manager.aws.topology.service;

import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.aws.topology.extractor.Ec2TopologyExtractor;
import com.cloudops.manager.aws.topology.extractor.SubnetVpcTopologyExtractor;
import com.cloudops.manager.aws.topology.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopologyQueryServiceTest {

    @Mock
    private AwsResourceDiscoveryService discoveryService;
    @Mock
    private AwsIdentityService identityService;

    private TopologyQueryService queryService;

    @BeforeEach
    void setUp() {
        TopologyGraphBuilder builder = new TopologyGraphBuilder(List.of(new Ec2TopologyExtractor(), new SubnetVpcTopologyExtractor()));
        queryService = new TopologyQueryService(builder, discoveryService, identityService);
        ReflectionTestUtils.setField(queryService, "defaultRegion", "us-east-1");
    }

    @Test
    @DisplayName("Should find deterministic path from EC2 to VPC through Subnet")
    void shouldFindPath() {
        when(identityService.getCurrentIdentity())
                .thenReturn(new CallerIdentity("123", "arn", "user"));

        InventorySummary summary = new InventorySummary("123", "us-east-1", 2, Map.of(),
                List.of(new Ec2InstanceResource("i-1", CloudResourceType.EC2_INSTANCE, "web", "us-east-1", "123", "running", "arn", Map.of(), Instant.now(), "t3.micro", "10.0.0.1", null, "vpc-1", "sub-1", "us-east-1a", "ami-1", Instant.now()),
                        new VpcResource("vpc-1", CloudResourceType.VPC, "main", "us-east-1", "123", "available", "arn", Map.of(), Instant.now(), "10.0.0.0/16", false, "dopt-1")), Instant.now());
        when(discoveryService.discoverAll(any())).thenReturn(summary);

        Ec2DetailResource ec2 = new Ec2DetailResource("i-1", "web", "arn", "123", "us-east-1", "t3.micro", "x86", "Linux", "Linux", "ami-1", null, Instant.now(), "running", null, null, "disabled", "us-east-1a", null, "default", "vpc-1", "sub-1", "10.0.0.1", null, "dns", "dns", List.of(), List.of(), Map.of(), Instant.now());
        when(discoveryService.getEc2InstanceDetail(eq("i-1"), any())).thenReturn(ec2);

        VpcDetailResource vpc = new VpcDetailResource("vpc-1", "arn", "123", "us-east-1", "available", "10.0.0.0/16", List.of(), List.of(), "dopt-1", "default", false, true, true, Map.of(), Instant.now());
        SubnetDetailResource sub = new SubnetDetailResource("sub-1", "arn", "vpc-1", "10.0.1.0/24", null, "us-east-1a", "use1-az1", "available", false, false, 250, true, Map.of());
        VpcTopologyResource vpcTopo = new VpcTopologyResource(vpc, List.of(sub), List.of(), List.of(), List.of(), List.of(), List.of(), Instant.now());
        when(discoveryService.getVpcTopology(eq("vpc-1"), any())).thenReturn(vpcTopo);

        String ec2NodeId = "123:us-east-1:EC2_INSTANCE:i-1";
        String vpcNodeId = "123:us-east-1:VPC:vpc-1";

        Optional<TopologyPath> pathOpt = queryService.findPath(ec2NodeId, vpcNodeId, "us-east-1");
        assertThat(pathOpt).isPresent();
        assertThat(pathOpt.get().length()).isEqualTo(2);
        assertThat(pathOpt.get().nodes()).hasSize(3);
    }
}