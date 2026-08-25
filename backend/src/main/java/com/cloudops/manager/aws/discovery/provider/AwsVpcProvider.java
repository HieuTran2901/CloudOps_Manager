package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.CloudResourceType;
import com.cloudops.manager.aws.discovery.model.InternetGatewayResource;
import com.cloudops.manager.aws.discovery.model.NatGatewayResource;
import com.cloudops.manager.aws.discovery.model.NetworkAclResource;
import com.cloudops.manager.aws.discovery.model.NetworkAclRule;
import com.cloudops.manager.aws.discovery.model.RouteDetail;
import com.cloudops.manager.aws.discovery.model.RouteTableDetailResource;
import com.cloudops.manager.aws.discovery.model.SubnetDetailResource;
import com.cloudops.manager.aws.discovery.model.VpcDetailResource;
import com.cloudops.manager.aws.discovery.model.VpcPeeringResource;
import com.cloudops.manager.aws.discovery.model.VpcResource;
import com.cloudops.manager.aws.discovery.model.VpcTopologyResource;
import com.cloudops.manager.common.exception.AwsErrorTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AwsVpcProvider implements VpcProvider {

    private static final Logger log = LoggerFactory.getLogger(AwsVpcProvider.class);
    private final Ec2Client ec2Client;

    public AwsVpcProvider(Ec2Client ec2Client) {
        this.ec2Client = ec2Client;
    }

    @Override
    public List<VpcResource> describeVpcs(String region, String accountId) {
        log.info("Discovering VPCs for account: {}, region: {}", accountId, region);
        List<VpcResource> results = new ArrayList<>();
        Instant discoveredAt = Instant.now();

        try {
            DescribeVpcsResponse response = ec2Client.describeVpcs(DescribeVpcsRequest.builder().build());
            for (Vpc vpc : response.vpcs()) {
                Map<String, String> tags = extractTags(vpc.tags());
                String name = tags.getOrDefault("Name", vpc.vpcId());
                String arn = "arn:aws:ec2:" + region + ":" + accountId + ":vpc/" + vpc.vpcId();
                String status = vpc.stateAsString() != null ? vpc.stateAsString().toUpperCase() : "AVAILABLE";

                results.add(new VpcResource(
                        vpc.vpcId(), CloudResourceType.VPC, name, region, accountId, status, arn,
                        tags, discoveredAt, vpc.cidrBlock(), vpc.isDefault(), vpc.dhcpOptionsId()
                ));
            }
            log.info("Discovered {} VPCs in region: {}", results.size(), region);
            return results;
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("EC2:DescribeVpcs", e, log);
        }
    }

    @Override
    public Optional<VpcDetailResource> getVpc(String vpcId, String region, String accountId) {
        log.info("Inspecting VPC: {} for account: {}, region: {}", vpcId, accountId, region);
        try {
            DescribeVpcsResponse response = ec2Client.describeVpcs(DescribeVpcsRequest.builder().vpcIds(vpcId).build());
            if (!response.hasVpcs() || response.vpcs().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(mapVpcDetail(response.vpcs().get(0), region, accountId));
        } catch (Ec2Exception e) {
            if ((e.statusCode() == 400 && "InvalidVpcID.NotFound".equalsIgnoreCase(e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "")) || e.statusCode() == 404) {
                return Optional.empty();
            }
            throw AwsErrorTranslator.translate("EC2:DescribeVpc:" + vpcId, e, log);
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("EC2:DescribeVpc:" + vpcId, e, log);
        }
    }

    @Override
    public Optional<VpcTopologyResource> getVpcTopology(String vpcId, String region, String accountId) {
        log.info("Aggregating VPC topology for: {}, region: {}", vpcId, region);
        Optional<VpcDetailResource> vpcOpt = getVpc(vpcId, region, accountId);
        if (vpcOpt.isEmpty()) return Optional.empty();

        try {
            return Optional.of(new VpcTopologyResource(
                    vpcOpt.get(), querySubnets(vpcId, region, accountId), queryRouteTables(vpcId),
                    queryInternetGateways(vpcId), queryNatGateways(vpcId), queryNetworkAcls(vpcId),
                    queryPeeringConnections(vpcId), Instant.now()
            ));
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("EC2:GetVpcTopology:" + vpcId, e, log);
        }
    }

    private VpcDetailResource mapVpcDetail(Vpc vpc, String region, String accountId) {
        Map<String, String> tags = extractTags(vpc.tags());
        String arn = "arn:aws:ec2:" + region + ":" + accountId + ":vpc/" + vpc.vpcId();
        List<String> secCidrs = vpc.hasCidrBlockAssociationSet() ? vpc.cidrBlockAssociationSet().stream().map(VpcCidrBlockAssociation::cidrBlock).toList() : List.of();
        List<String> ipv6Cidrs = vpc.hasIpv6CidrBlockAssociationSet() ? vpc.ipv6CidrBlockAssociationSet().stream().map(VpcIpv6CidrBlockAssociation::ipv6CidrBlock).toList() : List.of();

        Boolean dnsSupport = null;
        Boolean dnsHostnames = null;
        try {
            var dnsSupp = ec2Client.describeVpcAttribute(DescribeVpcAttributeRequest.builder().vpcId(vpc.vpcId()).attribute(VpcAttributeName.ENABLE_DNS_SUPPORT).build());
            dnsSupport = dnsSupp.enableDnsSupport() != null ? dnsSupp.enableDnsSupport().value() : null;
            var dnsHost = ec2Client.describeVpcAttribute(DescribeVpcAttributeRequest.builder().vpcId(vpc.vpcId()).attribute(VpcAttributeName.ENABLE_DNS_HOSTNAMES).build());
            dnsHostnames = dnsHost.enableDnsHostnames() != null ? dnsHost.enableDnsHostnames().value() : null;
        } catch (Exception e) {
            log.debug("DNS attributes error for {}: {}", vpc.vpcId(), e.getMessage());
        }

        return new VpcDetailResource(
                vpc.vpcId(), arn, accountId, region, vpc.stateAsString(), vpc.cidrBlock(),
                secCidrs, ipv6Cidrs, vpc.dhcpOptionsId(), vpc.instanceTenancyAsString(),
                vpc.isDefault(), dnsSupport, dnsHostnames, tags, Instant.now()
        );
    }

    private List<SubnetDetailResource> querySubnets(String vpcId, String region, String accountId) {
        List<SubnetDetailResource> list = new ArrayList<>();
        var paginator = ec2Client.describeSubnetsPaginator(DescribeSubnetsRequest.builder().filters(Filter.builder().name("vpc-id").values(vpcId).build()).build());
        for (var page : paginator) {
            for (Subnet s : page.subnets()) {
                String arn = "arn:aws:ec2:" + region + ":" + accountId + ":subnet/" + s.subnetId();
                String ipv6 = (s.hasIpv6CidrBlockAssociationSet() && !s.ipv6CidrBlockAssociationSet().isEmpty()) ? s.ipv6CidrBlockAssociationSet().get(0).ipv6CidrBlock() : null;
                list.add(new SubnetDetailResource(s.subnetId(), arn, s.vpcId(), s.cidrBlock(), ipv6, s.availabilityZone(), s.availabilityZoneId(), s.stateAsString(), s.mapPublicIpOnLaunch(), s.assignIpv6AddressOnCreation(), s.availableIpAddressCount(), s.defaultForAz(), extractTags(s.tags())));
            }
        }
        return list;
    }

    private List<RouteTableDetailResource> queryRouteTables(String vpcId) {
        List<RouteTableDetailResource> list = new ArrayList<>();
        var paginator = ec2Client.describeRouteTablesPaginator(DescribeRouteTablesRequest.builder().filters(Filter.builder().name("vpc-id").values(vpcId).build()).build());
        for (var page : paginator) {
            for (RouteTable rt : page.routeTables()) {
                boolean isMain = rt.hasAssociations() && rt.associations().stream().anyMatch(RouteTableAssociation::main);
                List<String> subnets = rt.hasAssociations() ? rt.associations().stream().filter(a -> a.subnetId() != null).map(RouteTableAssociation::subnetId).toList() : List.of();
                List<RouteDetail> routes = new ArrayList<>();
                if (rt.hasRoutes()) {
                    for (Route r : rt.routes()) {
                        String target = r.gatewayId() != null ? r.gatewayId() : (r.natGatewayId() != null ? r.natGatewayId() : (r.networkInterfaceId() != null ? r.networkInterfaceId() : r.transitGatewayId()));
                        String type = r.gatewayId() != null ? "GATEWAY" : (r.natGatewayId() != null ? "NAT_GATEWAY" : (r.networkInterfaceId() != null ? "NETWORK_INTERFACE" : "OTHER"));
                        routes.add(new RouteDetail(r.destinationCidrBlock(), r.destinationIpv6CidrBlock(), r.destinationPrefixListId(), target, type, r.stateAsString()));
                    }
                }
                list.add(new RouteTableDetailResource(rt.routeTableId(), rt.vpcId(), isMain, subnets, routes, extractTags(rt.tags())));
            }
        }
        return list;
    }

    private List<InternetGatewayResource> queryInternetGateways(String vpcId) {
        List<InternetGatewayResource> list = new ArrayList<>();
        var resp = ec2Client.describeInternetGateways(DescribeInternetGatewaysRequest.builder().filters(Filter.builder().name("attachment.vpc-id").values(vpcId).build()).build());
        if (resp.hasInternetGateways()) {
            for (InternetGateway igw : resp.internetGateways()) {
                String state = igw.hasAttachments() && !igw.attachments().isEmpty() ? igw.attachments().get(0).stateAsString() : "available";
                list.add(new InternetGatewayResource(igw.internetGatewayId(), vpcId, state, extractTags(igw.tags())));
            }
        }
        return list;
    }

    private List<NatGatewayResource> queryNatGateways(String vpcId) {
        List<NatGatewayResource> list = new ArrayList<>();
        var paginator = ec2Client.describeNatGatewaysPaginator(DescribeNatGatewaysRequest.builder().filter(Filter.builder().name("vpc-id").values(vpcId).build()).build());
        for (var page : paginator) {
            for (NatGateway ngw : page.natGateways()) {
                String pub = (ngw.hasNatGatewayAddresses() && !ngw.natGatewayAddresses().isEmpty()) ? ngw.natGatewayAddresses().get(0).publicIp() : null;
                String priv = (ngw.hasNatGatewayAddresses() && !ngw.natGatewayAddresses().isEmpty()) ? ngw.natGatewayAddresses().get(0).privateIp() : null;
                String eni = (ngw.hasNatGatewayAddresses() && !ngw.natGatewayAddresses().isEmpty()) ? ngw.natGatewayAddresses().get(0).networkInterfaceId() : null;
                list.add(new NatGatewayResource(ngw.natGatewayId(), ngw.vpcId(), ngw.subnetId(), ngw.stateAsString(), ngw.connectivityTypeAsString(), pub, priv, eni, extractTags(ngw.tags())));
            }
        }
        return list;
    }

    private List<NetworkAclResource> queryNetworkAcls(String vpcId) {
        List<NetworkAclResource> list = new ArrayList<>();
        var paginator = ec2Client.describeNetworkAclsPaginator(DescribeNetworkAclsRequest.builder().filters(Filter.builder().name("vpc-id").values(vpcId).build()).build());
        for (var page : paginator) {
            for (NetworkAcl acl : page.networkAcls()) {
                List<String> subnets = acl.hasAssociations() ? acl.associations().stream().map(NetworkAclAssociation::subnetId).toList() : List.of();
                List<NetworkAclRule> rules = new ArrayList<>();
                if (acl.hasEntries()) {
                    for (NetworkAclEntry e : acl.entries()) {
                        Integer pFrom = e.portRange() != null ? e.portRange().from() : null;
                        Integer pTo = e.portRange() != null ? e.portRange().to() : null;
                        Integer iCode = e.icmpTypeCode() != null ? e.icmpTypeCode().code() : null;
                        Integer iType = e.icmpTypeCode() != null ? e.icmpTypeCode().type() : null;
                        rules.add(new NetworkAclRule(e.ruleNumber(), e.protocol(), e.ruleActionAsString(), e.egress(), e.cidrBlock(), e.ipv6CidrBlock(), pFrom, pTo, iType, iCode));
                    }
                }
                list.add(new NetworkAclResource(acl.networkAclId(), acl.vpcId(), acl.isDefault(), subnets, rules, extractTags(acl.tags())));
            }
        }
        return list;
    }

    private List<VpcPeeringResource> queryPeeringConnections(String vpcId) {
        List<VpcPeeringResource> list = new ArrayList<>();
        try {
            var resp = ec2Client.describeVpcPeeringConnections(DescribeVpcPeeringConnectionsRequest.builder().filters(Filter.builder().name("requester-vpc-info.vpc-id").values(vpcId).build()).build());
            if (resp.hasVpcPeeringConnections()) {
                for (VpcPeeringConnection pc : resp.vpcPeeringConnections()) {
                    String reqVpc = pc.requesterVpcInfo() != null ? pc.requesterVpcInfo().vpcId() : null;
                    String accVpc = pc.accepterVpcInfo() != null ? pc.accepterVpcInfo().vpcId() : null;
                    String reqCidr = pc.requesterVpcInfo() != null ? pc.requesterVpcInfo().cidrBlock() : null;
                    String accCidr = pc.accepterVpcInfo() != null ? pc.accepterVpcInfo().cidrBlock() : null;
                    String status = pc.status() != null ? pc.status().codeAsString() : "unknown";
                    list.add(new VpcPeeringResource(pc.vpcPeeringConnectionId(), reqVpc, accVpc, reqCidr, accCidr, status, pc.status() != null ? pc.status().message() : null, extractTags(pc.tags())));
                }
            }
        } catch (Exception e) {
            log.debug("Peering query error for {}: {}", vpcId, e.getMessage());
        }
        return list;
    }

    private Map<String, String> extractTags(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) return Collections.emptyMap();
        return tags.stream().collect(Collectors.toMap(Tag::key, Tag::value, (k1, k2) -> k1));
    }
}