package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.CloudResourceType;
import com.cloudops.manager.aws.discovery.model.ComputeResourceAttachment;
import com.cloudops.manager.aws.discovery.model.DatabaseResourceAttachment;
import com.cloudops.manager.aws.discovery.model.IpPermissionRule;
import com.cloudops.manager.aws.discovery.model.NetworkInterfaceAttachment;
import com.cloudops.manager.aws.discovery.model.SecurityGroupAttachment;
import com.cloudops.manager.aws.discovery.model.SecurityGroupDetailResource;
import com.cloudops.manager.aws.discovery.model.SecurityGroupReference;
import com.cloudops.manager.aws.discovery.model.SecurityGroupResource;
import com.cloudops.manager.aws.discovery.model.SecurityGroupRuleDetail;
import com.cloudops.manager.aws.discovery.model.SecurityGroupTopologyResource;
import com.cloudops.manager.common.exception.AwsErrorTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.rds.model.DBInstance;
import software.amazon.awssdk.services.rds.model.DescribeDbInstancesRequest;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AwsSecurityGroupProvider implements SecurityGroupProvider {

    private static final Logger log = LoggerFactory.getLogger(AwsSecurityGroupProvider.class);
    private final Ec2Client ec2Client;
    private final RdsClient rdsClient;

    public AwsSecurityGroupProvider(Ec2Client ec2Client, RdsClient rdsClient) {
        this.ec2Client = ec2Client;
        this.rdsClient = rdsClient;
    }

    @Override
    public List<SecurityGroupResource> describeSecurityGroups(String region, String accountId) {
        log.info("Discovering Security Groups for account: {}, region: {}", accountId, region);
        List<SecurityGroupResource> results = new ArrayList<>();
        Instant discoveredAt = Instant.now();

        try {
            var paginator = ec2Client.describeSecurityGroupsPaginator(DescribeSecurityGroupsRequest.builder().build());
            for (var response : paginator) {
                for (SecurityGroup sg : response.securityGroups()) {
                    Map<String, String> tags = extractTags(sg.tags());
                    String name = tags.getOrDefault("Name", sg.groupName());
                    String arn = "arn:aws:ec2:" + region + ":" + accountId + ":security-group/" + sg.groupId();

                    results.add(new SecurityGroupResource(
                            sg.groupId(), CloudResourceType.SECURITY_GROUP, name, region, accountId,
                            "ACTIVE", arn, tags, discoveredAt, sg.description(), sg.vpcId(),
                            mapSimpleRules(sg.ipPermissions()), mapSimpleRules(sg.ipPermissionsEgress())
                    ));
                }
            }
            log.info("Discovered {} Security Groups in region: {}", results.size(), region);
            return results;
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("EC2:DescribeSecurityGroups", e, log);
        }
    }

    @Override
    public Optional<SecurityGroupDetailResource> getSecurityGroup(String securityGroupId, String region, String accountId) {
        log.info("Inspecting Security Group: {} for account: {}, region: {}", securityGroupId, accountId, region);
        try {
            var resp = ec2Client.describeSecurityGroups(DescribeSecurityGroupsRequest.builder().groupIds(securityGroupId).build());
            if (!resp.hasSecurityGroups() || resp.securityGroups().isEmpty()) return Optional.empty();
            SecurityGroup sg = resp.securityGroups().get(0);
            return Optional.of(mapSecurityGroupDetail(sg, region, accountId));
        } catch (Ec2Exception e) {
            if ((e.statusCode() == 400 && "InvalidGroup.NotFound".equalsIgnoreCase(e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "")) || e.statusCode() == 404) {
                return Optional.empty();
            }
            throw AwsErrorTranslator.translate("EC2:DescribeSecurityGroup:" + securityGroupId, e, log);
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("EC2:DescribeSecurityGroup:" + securityGroupId, e, log);
        }
    }

    @Override
    public Optional<SecurityGroupTopologyResource> getSecurityGroupTopology(String securityGroupId, String region, String accountId) {
        log.info("Aggregating Security Group topology for: {}, region: {}", securityGroupId, region);
        Optional<SecurityGroupDetailResource> sgOpt = getSecurityGroup(securityGroupId, region, accountId);
        if (sgOpt.isEmpty()) return Optional.empty();

        try {
            List<NetworkInterfaceAttachment> enis = queryEnis(securityGroupId);
            List<ComputeResourceAttachment> compute = queryComputeInstances(securityGroupId);
            List<DatabaseResourceAttachment> databases = queryDatabases(securityGroupId);

            SecurityGroupAttachment attachments = new SecurityGroupAttachment(enis, compute, databases);
            return Optional.of(new SecurityGroupTopologyResource(sgOpt.get(), attachments, Instant.now()));
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("EC2:GetSecurityGroupTopology:" + securityGroupId, e, log);
        }
    }

    private SecurityGroupDetailResource mapSecurityGroupDetail(SecurityGroup sg, String region, String accountId) {
        String arn = "arn:aws:ec2:" + region + ":" + accountId + ":security-group/" + sg.groupId();
        return new SecurityGroupDetailResource(
                sg.groupId(), arn, sg.groupName(), sg.description(), sg.vpcId(), sg.ownerId(),
                accountId, region, mapDetailedRules(sg.ipPermissions()), mapDetailedRules(sg.ipPermissionsEgress()),
                extractTags(sg.tags()), Instant.now()
        );
    }

    private List<SecurityGroupRuleDetail> mapDetailedRules(List<IpPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) return List.of();
        return permissions.stream().map(p -> {
            List<String> v4 = p.hasIpRanges() ? p.ipRanges().stream().map(IpRange::cidrIp).toList() : List.of();
            List<String> v6 = p.hasIpv6Ranges() ? p.ipv6Ranges().stream().map(Ipv6Range::cidrIpv6).toList() : List.of();
            List<String> pls = p.hasPrefixListIds() ? p.prefixListIds().stream().map(PrefixListId::prefixListId).toList() : List.of();
            List<SecurityGroupReference> refs = p.hasUserIdGroupPairs() ? p.userIdGroupPairs().stream().map(u -> new SecurityGroupReference(u.groupId(), u.userId(), u.vpcId(), u.vpcPeeringConnectionId(), u.description())).toList() : List.of();
            String desc = (p.hasIpRanges() && !p.ipRanges().isEmpty()) ? p.ipRanges().get(0).description() : null;
            return new SecurityGroupRuleDetail(p.ipProtocol(), p.fromPort(), p.toPort(), v4, v6, pls, refs, desc);
        }).toList();
    }

    private List<IpPermissionRule> mapSimpleRules(List<IpPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) return List.of();
        return permissions.stream().map(p -> new IpPermissionRule(
                p.ipProtocol(), p.fromPort(), p.toPort(),
                p.hasIpRanges() ? p.ipRanges().stream().map(IpRange::cidrIp).toList() : List.of(),
                p.hasUserIdGroupPairs() ? p.userIdGroupPairs().stream().map(UserIdGroupPair::groupId).toList() : List.of()
        )).toList();
    }

    private List<NetworkInterfaceAttachment> queryEnis(String securityGroupId) {
        List<NetworkInterfaceAttachment> list = new ArrayList<>();
        var paginator = ec2Client.describeNetworkInterfacesPaginator(DescribeNetworkInterfacesRequest.builder()
                .filters(Filter.builder().name("group-id").values(securityGroupId).build()).build());
        for (var page : paginator) {
            for (NetworkInterface eni : page.networkInterfaces()) {
                String status = eni.attachment() != null ? eni.attachment().statusAsString() : "unattached";
                list.add(new NetworkInterfaceAttachment(eni.networkInterfaceId(), eni.subnetId(), eni.vpcId(), eni.privateIpAddress(), eni.interfaceTypeAsString(), status));
            }
        }
        return list;
    }

    private List<ComputeResourceAttachment> queryComputeInstances(String securityGroupId) {
        List<ComputeResourceAttachment> list = new ArrayList<>();
        var paginator = ec2Client.describeInstancesPaginator(DescribeInstancesRequest.builder()
                .filters(Filter.builder().name("instance.group-id").values(securityGroupId).build()).build());
        for (var page : paginator) {
            for (Reservation r : page.reservations()) {
                for (Instance i : r.instances()) {
                    String name = i.hasTags() ? i.tags().stream().filter(t -> "Name".equalsIgnoreCase(t.key())).map(Tag::value).findFirst().orElse(i.instanceId()) : i.instanceId();
                    String state = i.state() != null ? i.state().nameAsString() : "unknown";
                    list.add(new ComputeResourceAttachment(i.instanceId(), name, i.instanceTypeAsString(), state));
                }
            }
        }
        return list;
    }

    private List<DatabaseResourceAttachment> queryDatabases(String securityGroupId) {
        List<DatabaseResourceAttachment> list = new ArrayList<>();
        try {
            var paginator = rdsClient.describeDBInstancesPaginator(DescribeDbInstancesRequest.builder().build());
            for (var page : paginator) {
                for (DBInstance db : page.dbInstances()) {
                    boolean attached = db.hasVpcSecurityGroups() && db.vpcSecurityGroups().stream().anyMatch(sg -> securityGroupId.equals(sg.vpcSecurityGroupId()));
                    if (attached) {
                        list.add(new DatabaseResourceAttachment(db.dbInstanceIdentifier(), db.engine(), db.dbInstanceStatus()));
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not query RDS attachments for security group {}: {}", securityGroupId, e.getMessage());
        }
        return list;
    }

    private Map<String, String> extractTags(List<Tag> tags) {
        if (tags == null || tags.isEmpty()) return Collections.emptyMap();
        return tags.stream().collect(Collectors.toMap(Tag::key, Tag::value, (k1, k2) -> k1));
    }
}