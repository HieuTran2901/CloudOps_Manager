package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.CloudResourceType;
import com.cloudops.manager.aws.discovery.model.SecurityGroupDetailResource;
import com.cloudops.manager.aws.discovery.model.SecurityGroupResource;
import com.cloudops.manager.aws.discovery.model.SecurityGroupTopologyResource;
import com.cloudops.manager.aws.discovery.model.VpcDetailResource;
import com.cloudops.manager.aws.discovery.model.VpcResource;
import com.cloudops.manager.aws.discovery.model.VpcTopologyResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInternetGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNatGatewaysResponse;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkAclsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkAclsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeNetworkInterfacesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeRouteTablesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSecurityGroupsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcAttributeRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcAttributeResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcPeeringConnectionsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcPeeringConnectionsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVpcsResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InternetGateway;
import software.amazon.awssdk.services.ec2.model.InternetGatewayAttachment;
import software.amazon.awssdk.services.ec2.model.IpPermission;
import software.amazon.awssdk.services.ec2.model.IpRange;
import software.amazon.awssdk.services.ec2.model.NatGateway;
import software.amazon.awssdk.services.ec2.model.NatGatewayAddress;
import software.amazon.awssdk.services.ec2.model.NetworkAcl;
import software.amazon.awssdk.services.ec2.model.NetworkAclEntry;
import software.amazon.awssdk.services.ec2.model.NetworkInterface;
import software.amazon.awssdk.services.ec2.model.NetworkInterfaceAttachment;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Route;
import software.amazon.awssdk.services.ec2.model.RouteTable;
import software.amazon.awssdk.services.ec2.model.RouteTableAssociation;
import software.amazon.awssdk.services.ec2.model.SecurityGroup;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.UserIdGroupPair;
import software.amazon.awssdk.services.ec2.model.Vpc;
import software.amazon.awssdk.services.ec2.model.VpcPeeringConnection;
import software.amazon.awssdk.services.ec2.model.VpcPeeringConnectionVpcInfo;
import software.amazon.awssdk.services.ec2.paginators.DescribeInstancesIterable;
import software.amazon.awssdk.services.ec2.paginators.DescribeNatGatewaysIterable;
import software.amazon.awssdk.services.ec2.paginators.DescribeNetworkAclsIterable;
import software.amazon.awssdk.services.ec2.paginators.DescribeNetworkInterfacesIterable;
import software.amazon.awssdk.services.ec2.paginators.DescribeRouteTablesIterable;
import software.amazon.awssdk.services.ec2.paginators.DescribeSecurityGroupsIterable;
import software.amazon.awssdk.services.ec2.paginators.DescribeSubnetsIterable;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesResponse;
import software.amazon.awssdk.services.rds.model.VpcSecurityGroupMembership;
import software.amazon.awssdk.services.rds.paginators.DescribeDBInstancesIterable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsNetworkProviderTest {

    @Mock
    private Ec2Client ec2Client;
    @Mock
    private RdsClient rdsClient;

    private AwsVpcProvider vpcProvider;
    private AwsSecurityGroupProvider securityGroupProvider;

    @BeforeEach
    void setUp() {
        vpcProvider = new AwsVpcProvider(ec2Client);
        securityGroupProvider = new AwsSecurityGroupProvider(ec2Client, rdsClient);
    }

    @Test
    @DisplayName("Should successfully discover VPCs")
    void shouldDiscoverVpcs() {
        Vpc vpc = Vpc.builder()
                .vpcId("vpc-12345")
                .cidrBlock("10.0.0.0/16")
                .isDefault(false)
                .dhcpOptionsId("dopt-1")
                .state("available")
                .tags(Tag.builder().key("Name").value("Main-VPC").build())
                .build();

        when(ec2Client.describeVpcs(any(DescribeVpcsRequest.class)))
                .thenReturn(DescribeVpcsResponse.builder().vpcs(vpc).build());

        List<VpcResource> vpcs = vpcProvider.describeVpcs("us-east-1", "123456789012");

        assertThat(vpcs).hasSize(1);
        assertThat(vpcs.get(0).resourceId()).isEqualTo("vpc-12345");
        assertThat(vpcs.get(0).resourceType()).isEqualTo(CloudResourceType.VPC);
        assertThat(vpcs.get(0).cidrBlock()).isEqualTo("10.0.0.0/16");
    }

    @Test
    @DisplayName("Should inspect VPC detail with DNS attributes")
    void shouldInspectVpcDetail() {
        Vpc vpc = Vpc.builder()
                .vpcId("vpc-12345")
                .cidrBlock("10.0.0.0/16")
                .isDefault(true)
                .dhcpOptionsId("dopt-1")
                .state("available")
                .tags(Tag.builder().key("Name").value("Default-VPC").build())
                .build();

        when(ec2Client.describeVpcs(any(DescribeVpcsRequest.class)))
                .thenReturn(DescribeVpcsResponse.builder().vpcs(vpc).build());

        when(ec2Client.describeVpcAttribute(any(DescribeVpcAttributeRequest.class)))
                .thenReturn(DescribeVpcAttributeResponse.builder().build());

        Optional<VpcDetailResource> detail = vpcProvider.getVpc("vpc-12345", "us-east-1", "123456789012");

        assertThat(detail).isPresent();
        assertThat(detail.get().vpcId()).isEqualTo("vpc-12345");
        assertThat(detail.get().isDefault()).isTrue();
    }

    @Test
    @DisplayName("Should aggregate complete VPC topology")
    void shouldAggregateVpcTopology() {
        Vpc vpc = Vpc.builder().vpcId("vpc-100").cidrBlock("10.0.0.0/16").state("available").build();
        when(ec2Client.describeVpcs(any(DescribeVpcsRequest.class)))
                .thenReturn(DescribeVpcsResponse.builder().vpcs(vpc).build());
        when(ec2Client.describeVpcAttribute(any(DescribeVpcAttributeRequest.class)))
                .thenReturn(DescribeVpcAttributeResponse.builder().build());

        Subnet subnet = Subnet.builder().subnetId("subnet-1").vpcId("vpc-100").cidrBlock("10.0.1.0/24").availabilityZone("us-east-1a").state("available").build();
        DescribeSubnetsIterable subPaginator = mock(DescribeSubnetsIterable.class);
        when(subPaginator.iterator()).thenReturn(List.of(DescribeSubnetsResponse.builder().subnets(subnet).build()).iterator());
        when(ec2Client.describeSubnetsPaginator(any(DescribeSubnetsRequest.class))).thenReturn(subPaginator);

        RouteTable rt = RouteTable.builder().routeTableId("rtb-1").vpcId("vpc-100")
                .associations(RouteTableAssociation.builder().main(true).build())
                .routes(Route.builder().destinationCidrBlock("0.0.0.0/0").gatewayId("igw-1").state("active").build())
                .build();
        DescribeRouteTablesIterable rtPaginator = mock(DescribeRouteTablesIterable.class);
        when(rtPaginator.iterator()).thenReturn(List.of(DescribeRouteTablesResponse.builder().routeTables(rt).build()).iterator());
        when(ec2Client.describeRouteTablesPaginator(any(DescribeRouteTablesRequest.class))).thenReturn(rtPaginator);

        InternetGateway igw = InternetGateway.builder().internetGatewayId("igw-1")
                .attachments(InternetGatewayAttachment.builder().vpcId("vpc-100").state("available").build()).build();
        when(ec2Client.describeInternetGateways(any(DescribeInternetGatewaysRequest.class)))
                .thenReturn(DescribeInternetGatewaysResponse.builder().internetGateways(igw).build());

        NatGateway nat = NatGateway.builder().natGatewayId("nat-1").vpcId("vpc-100").subnetId("subnet-1").state("available")
                .natGatewayAddresses(NatGatewayAddress.builder().publicIp("1.2.3.4").build()).build();
        DescribeNatGatewaysIterable natPaginator = mock(DescribeNatGatewaysIterable.class);
        when(natPaginator.iterator()).thenReturn(List.of(DescribeNatGatewaysResponse.builder().natGateways(nat).build()).iterator());
        when(ec2Client.describeNatGatewaysPaginator(any(DescribeNatGatewaysRequest.class))).thenReturn(natPaginator);

        NetworkAcl nacl = NetworkAcl.builder().networkAclId("acl-1").vpcId("vpc-100").isDefault(true)
                .entries(NetworkAclEntry.builder().ruleNumber(100).protocol("-1").ruleAction("allow").egress(false).cidrBlock("0.0.0.0/0").build()).build();
        DescribeNetworkAclsIterable aclPaginator = mock(DescribeNetworkAclsIterable.class);
        when(aclPaginator.iterator()).thenReturn(List.of(DescribeNetworkAclsResponse.builder().networkAcls(nacl).build()).iterator());
        when(ec2Client.describeNetworkAclsPaginator(any(DescribeNetworkAclsRequest.class))).thenReturn(aclPaginator);

        VpcPeeringConnection peering = VpcPeeringConnection.builder().vpcPeeringConnectionId("pcx-1")
                .requesterVpcInfo(VpcPeeringConnectionVpcInfo.builder().vpcId("vpc-100").cidrBlock("10.0.0.0/16").build())
                .accepterVpcInfo(VpcPeeringConnectionVpcInfo.builder().vpcId("vpc-200").cidrBlock("10.1.0.0/16").build())
                .build();
        when(ec2Client.describeVpcPeeringConnections(any(DescribeVpcPeeringConnectionsRequest.class)))
                .thenReturn(DescribeVpcPeeringConnectionsResponse.builder().vpcPeeringConnections(peering).build());

        Optional<VpcTopologyResource> topology = vpcProvider.getVpcTopology("vpc-100", "us-east-1", "123456789012");

        assertThat(topology).isPresent();
        assertThat(topology.get().vpc().vpcId()).isEqualTo("vpc-100");
    }

    @Test
    @DisplayName("Should successfully discover Security Groups via paginator")
    void shouldDiscoverSecurityGroups() {
        SecurityGroup sg = SecurityGroup.builder()
                .groupId("sg-12345")
                .groupName("web-sg")
                .description("Web Security Group")
                .vpcId("vpc-12345")
                .ipPermissions(IpPermission.builder()
                        .ipProtocol("tcp")
                        .fromPort(80)
                        .toPort(80)
                        .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").build())
                        .build())
                .build();

        DescribeSecurityGroupsResponse page = DescribeSecurityGroupsResponse.builder().securityGroups(sg).build();
        DescribeSecurityGroupsIterable mockPaginator = mock(DescribeSecurityGroupsIterable.class);
        when(mockPaginator.iterator()).thenReturn(List.of(page).iterator());
        when(ec2Client.describeSecurityGroupsPaginator(any(DescribeSecurityGroupsRequest.class))).thenReturn(mockPaginator);

        List<SecurityGroupResource> sgs = securityGroupProvider.describeSecurityGroups("us-east-1", "123456789012");

        assertThat(sgs).hasSize(1);
        assertThat(sgs.get(0).resourceId()).isEqualTo("sg-12345");
        assertThat(sgs.get(0).resourceType()).isEqualTo(CloudResourceType.SECURITY_GROUP);
    }

    @Test
    @DisplayName("Should inspect Security Group details with rich rules")
    void shouldInspectSecurityGroupDetail() {
        SecurityGroup sg = SecurityGroup.builder()
                .groupId("sg-web")
                .groupName("web-sg")
                .description("HTTP SG")
                .vpcId("vpc-1")
                .ownerId("123456789012")
                .ipPermissions(IpPermission.builder()
                        .ipProtocol("tcp")
                        .fromPort(443)
                        .toPort(443)
                        .ipRanges(IpRange.builder().cidrIp("0.0.0.0/0").description("HTTPS public").build())
                        .userIdGroupPairs(UserIdGroupPair.builder().groupId("sg-lb").build())
                        .build())
                .build();

        when(ec2Client.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
                .thenReturn(DescribeSecurityGroupsResponse.builder().securityGroups(sg).build());

        Optional<SecurityGroupDetailResource> detail = securityGroupProvider.getSecurityGroup("sg-web", "us-east-1", "123456789012");

        assertThat(detail).isPresent();
        SecurityGroupDetailResource res = detail.get();
        assertThat(res.securityGroupId()).isEqualTo("sg-web");
        assertThat(res.inboundRules()).hasSize(1);
        assertThat(res.inboundRules().get(0).fromPort()).isEqualTo(443);
        assertThat(res.inboundRules().get(0).referencedSecurityGroups()).hasSize(1);
        assertThat(res.inboundRules().get(0).referencedSecurityGroups().get(0).groupId()).isEqualTo("sg-lb");
    }

    @Test
    @DisplayName("Should aggregate Security Group topology with ENIs, compute, and RDS attachments")
    void shouldAggregateSecurityGroupTopology() {
        SecurityGroup sg = SecurityGroup.builder().groupId("sg-app").groupName("app-sg").vpcId("vpc-1").build();
        when(ec2Client.describeSecurityGroups(any(DescribeSecurityGroupsRequest.class)))
                .thenReturn(DescribeSecurityGroupsResponse.builder().securityGroups(sg).build());

        // ENI
        NetworkInterface eni = NetworkInterface.builder().networkInterfaceId("eni-1").subnetId("subnet-1").vpcId("vpc-1").privateIpAddress("10.0.1.10")
                .interfaceType("interface").attachment(NetworkInterfaceAttachment.builder().status("attached").build()).build();
        DescribeNetworkInterfacesIterable eniPaginator = mock(DescribeNetworkInterfacesIterable.class);
        when(eniPaginator.iterator()).thenReturn(List.of(DescribeNetworkInterfacesResponse.builder().networkInterfaces(eni).build()).iterator());
        when(ec2Client.describeNetworkInterfacesPaginator(any(DescribeNetworkInterfacesRequest.class))).thenReturn(eniPaginator);

        // Compute
        Instance instance = Instance.builder().instanceId("i-app").instanceType("t3.micro").state(InstanceState.builder().name("running").build()).build();
        Reservation reservation = Reservation.builder().instances(instance).build();
        DescribeInstancesIterable instPaginator = mock(DescribeInstancesIterable.class);
        when(instPaginator.iterator()).thenReturn(List.of(DescribeInstancesResponse.builder().reservations(reservation).build()).iterator());
        when(ec2Client.describeInstancesPaginator(any(DescribeInstancesRequest.class))).thenReturn(instPaginator);

        // RDS
        DBInstance db = DBInstance.builder().dbInstanceIdentifier("db-prod").engine("postgres").dbInstanceStatus("available")
                .vpcSecurityGroups(VpcSecurityGroupMembership.builder().vpcSecurityGroupId("sg-app").status("active").build()).build();
        DescribeDBInstancesIterable rdsPaginator = mock(DescribeDBInstancesIterable.class);
        when(rdsPaginator.iterator()).thenReturn(List.of(DescribeDbInstancesResponse.builder().dbInstances(db).build()).iterator());
        when(rdsClient.describeDBInstancesPaginator(any(DescribeDbInstancesRequest.class))).thenReturn(rdsPaginator);

        Optional<SecurityGroupTopologyResource> topology = securityGroupProvider.getSecurityGroupTopology("sg-app", "us-east-1", "123456789012");

        assertThat(topology).isPresent();
        SecurityGroupTopologyResource res = topology.get();
        assertThat(res.securityGroup().securityGroupId()).isEqualTo("sg-app");
        assertThat(res.attachments().networkInterfaces()).hasSize(1);
        assertThat(res.attachments().networkInterfaces().get(0).networkInterfaceId()).isEqualTo("eni-1");
        assertThat(res.attachments().computeInstances()).hasSize(1);
        assertThat(res.attachments().computeInstances().get(0).instanceId()).isEqualTo("i-app");
        assertThat(res.attachments().databaseInstances()).hasSize(1);
        assertThat(res.attachments().databaseInstances().get(0).dbInstanceIdentifier()).isEqualTo("db-prod");
    }
}