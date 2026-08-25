package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.CloudResourceType;
import com.cloudops.manager.aws.discovery.model.Ec2DetailResource;
import com.cloudops.manager.aws.discovery.model.Ec2EbsAttachment;
import com.cloudops.manager.aws.discovery.model.Ec2InstanceResource;
import com.cloudops.manager.aws.discovery.model.Ec2NetworkInterfaceDetail;
import com.cloudops.manager.common.exception.AwsErrorTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterface;
import software.amazon.awssdk.services.ec2.model.InstancePrivateIpAddress;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AwsEc2Provider implements Ec2Provider {

    private static final Logger log = LoggerFactory.getLogger(AwsEc2Provider.class);
    private final Ec2Client ec2Client;

    public AwsEc2Provider(Ec2Client ec2Client) {
        this.ec2Client = ec2Client;
    }

    @Override
    public List<Ec2InstanceResource> describeInstances(String region, String accountId) {
        log.info("Discovering EC2 instances for account: {}, region: {}", accountId, region);
        List<Ec2InstanceResource> results = new ArrayList<>();
        Instant discoveredAt = Instant.now();

        try {
            var paginator = ec2Client.describeInstancesPaginator(DescribeInstancesRequest.builder().build());
            for (var response : paginator) {
                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        Map<String, String> tags = extractTags(instance);
                        String name = tags.getOrDefault("Name", instance.instanceId());
                        String arn = "arn:aws:ec2:" + region + ":" + accountId + ":instance/" + instance.instanceId();
                        String state = instance.state() != null ? instance.state().nameAsString() : "UNKNOWN";
                        String type = instance.instanceType() != null ? instance.instanceType().toString() : "UNKNOWN";
                        String az = instance.placement() != null ? instance.placement().availabilityZone() : null;

                        results.add(new Ec2InstanceResource(
                                instance.instanceId(), CloudResourceType.EC2_INSTANCE, name, region, accountId,
                                state, arn, tags, discoveredAt, type, instance.privateIpAddress(),
                                instance.publicIpAddress(), instance.vpcId(), instance.subnetId(), az,
                                instance.imageId(), instance.launchTime()
                        ));
                    }
                }
            }
            log.info("Discovered {} EC2 instances in region: {}", results.size(), region);
            return results;
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("EC2:DescribeInstances", e, log);
        }
    }

    @Override
    public Optional<Ec2DetailResource> getInstance(String instanceId, String region, String accountId) {
        log.info("Inspecting EC2 instance: {} in region: {}", instanceId, region);
        try {
            DescribeInstancesResponse response = ec2Client.describeInstances(
                    DescribeInstancesRequest.builder().instanceIds(instanceId).build()
            );

            if (response.reservations().isEmpty() || response.reservations().get(0).instances().isEmpty()) {
                return Optional.empty();
            }

            Instance instance = response.reservations().get(0).instances().get(0);
            Map<String, String> tags = extractTags(instance);
            String name = tags.getOrDefault("Name", instance.instanceId());
            String arn = "arn:aws:ec2:" + region + ":" + accountId + ":instance/" + instance.instanceId();

            List<Ec2EbsAttachment> ebsAttachments = inspectEbsVolumes(instance);
            List<Ec2NetworkInterfaceDetail> networkInterfaces = inspectNetworkInterfaces(instance);

            String architecture = instance.architecture() != null ? instance.architecture().toString() : null;
            String platform = instance.platform() != null ? instance.platform().toString() : "linux";
            String state = instance.state() != null ? instance.state().nameAsString() : "UNKNOWN";
            String stateReason = instance.stateReason() != null ? instance.stateReason().message() : null;
            String lifecycle = instance.instanceLifecycle() != null ? instance.instanceLifecycle().toString() : "on-demand";
            String monitoring = instance.monitoring() != null && instance.monitoring().state() != null ? instance.monitoring().state().toString() : null;
            String az = instance.placement() != null ? instance.placement().availabilityZone() : null;
            String placementGroup = instance.placement() != null ? instance.placement().groupName() : null;
            String tenancy = instance.placement() != null && instance.placement().tenancy() != null ? instance.placement().tenancy().toString() : null;
            String instanceType = instance.instanceType() != null ? instance.instanceType().toString() : null;

            return Optional.of(new Ec2DetailResource(
                    instance.instanceId(), name, arn, accountId, region, instanceType, architecture, platform,
                    instance.platformDetails(), instance.imageId(), instance.kernelId(), instance.launchTime(),
                    state, stateReason, lifecycle, monitoring, az, placementGroup, tenancy, instance.vpcId(),
                    instance.subnetId(), instance.privateIpAddress(), instance.publicIpAddress(),
                    instance.privateDnsName(), instance.publicDnsName(), ebsAttachments, networkInterfaces,
                    tags, Instant.now()
            ));
        } catch (Ec2Exception e) {
            String errorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "";
            if ("InvalidInstanceID.NotFound".equalsIgnoreCase(errorCode) || "InvalidInstanceID.Malformed".equalsIgnoreCase(errorCode)) {
                log.info("EC2 instance {} not found or malformed ID: {}", instanceId, errorCode);
                return Optional.empty();
            }
            throw AwsErrorTranslator.translate("EC2:GetInstance", e, log);
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("EC2:GetInstance", e, log);
        }
    }

    private Map<String, String> extractTags(Instance instance) {
        if (!instance.hasTags()) return Collections.emptyMap();
        return instance.tags().stream().collect(Collectors.toMap(Tag::key, Tag::value, (k1, k2) -> k1));
    }

    private List<Ec2EbsAttachment> inspectEbsVolumes(Instance instance) {
        if (!instance.hasBlockDeviceMappings() || instance.blockDeviceMappings().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> volumeIds = instance.blockDeviceMappings().stream()
                .filter(bdm -> bdm.ebs() != null && bdm.ebs().volumeId() != null && !bdm.ebs().volumeId().isBlank())
                .map(bdm -> bdm.ebs().volumeId())
                .toList();

        Map<String, Volume> volumeMap = new HashMap<>();
        if (!volumeIds.isEmpty()) {
            try {
                DescribeVolumesResponse volResponse = ec2Client.describeVolumes(
                        DescribeVolumesRequest.builder().volumeIds(volumeIds).build()
                );
                for (Volume v : volResponse.volumes()) {
                    volumeMap.put(v.volumeId(), v);
                }
            } catch (Exception e) {
                log.warn("Failed to retrieve enriched volume details for volumes: {}", volumeIds, e);
            }
        }

        List<Ec2EbsAttachment> attachments = new ArrayList<>();
        for (InstanceBlockDeviceMapping bdm : instance.blockDeviceMappings()) {
            if (bdm.ebs() == null) continue;
            String volId = bdm.ebs().volumeId();
            Volume vol = volumeMap.get(volId);

            attachments.add(new Ec2EbsAttachment(
                    volId, bdm.deviceName(), vol != null ? vol.size() : null,
                    vol != null && vol.volumeType() != null ? vol.volumeType().toString() : null,
                    vol != null ? vol.iops() : null, vol != null ? vol.throughput() : null,
                    vol != null ? vol.encrypted() : null, vol != null && vol.state() != null ? vol.state().toString() : null,
                    vol != null ? vol.availabilityZone() : null, bdm.ebs().attachTime(), bdm.ebs().deleteOnTermination()
            ));
        }
        return attachments;
    }

    private List<Ec2NetworkInterfaceDetail> inspectNetworkInterfaces(Instance instance) {
        if (!instance.hasNetworkInterfaces() || instance.networkInterfaces().isEmpty()) {
            return Collections.emptyList();
        }

        List<Ec2NetworkInterfaceDetail> enis = new ArrayList<>();
        for (InstanceNetworkInterface eni : instance.networkInterfaces()) {
            List<String> privateIps = eni.hasPrivateIpAddresses()
                    ? eni.privateIpAddresses().stream().map(InstancePrivateIpAddress::privateIpAddress).toList()
                    : Collections.emptyList();
            List<String> sgIds = eni.hasGroups()
                    ? eni.groups().stream().map(GroupIdentifier::groupId).toList()
                    : Collections.emptyList();
            List<String> sgNames = eni.hasGroups()
                    ? eni.groups().stream().map(GroupIdentifier::groupName).toList()
                    : Collections.emptyList();
            String publicIp = eni.association() != null ? eni.association().publicIp() : null;
            String status = eni.status() != null ? eni.status().toString() : null;

            enis.add(new Ec2NetworkInterfaceDetail(
                    eni.networkInterfaceId(), eni.subnetId(), eni.vpcId(), eni.privateIpAddress(),
                    privateIps, publicIp, eni.macAddress(), sgIds, sgNames, status, eni.description(),
                    eni.interfaceType()
            ));
        }
        return enis;
    }
}