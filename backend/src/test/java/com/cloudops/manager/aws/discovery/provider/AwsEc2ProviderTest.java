package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.CloudResourceType;
import com.cloudops.manager.aws.discovery.model.Ec2DetailResource;
import com.cloudops.manager.aws.discovery.model.Ec2InstanceResource;
import com.cloudops.manager.common.exception.AwsAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.EbsInstanceBlockDevice;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.InstanceNetworkInterface;
import software.amazon.awssdk.services.ec2.model.InstancePrivateIpAddress;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Placement;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeType;
import software.amazon.awssdk.services.ec2.paginators.DescribeInstancesIterable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsEc2ProviderTest {

    @Mock
    private Ec2Client ec2Client;

    private AwsEc2Provider ec2Provider;

    @BeforeEach
    void setUp() {
        ec2Provider = new AwsEc2Provider(ec2Client);
    }

    @Test
    @DisplayName("Should successfully discover EC2 instances across pages")
    void shouldDiscoverEc2InstancesWithPagination() {
        Instance instance = Instance.builder()
                .instanceId("i-0123456789abcdef0")
                .instanceType(InstanceType.T3_MICRO)
                .state(InstanceState.builder().name("running").build())
                .privateIpAddress("10.0.1.50")
                .publicIpAddress("54.210.10.20")
                .vpcId("vpc-11112222")
                .subnetId("subnet-33334444")
                .placement(Placement.builder().availabilityZone("us-east-1a").build())
                .imageId("ami-0abcdef1234567890")
                .launchTime(Instant.now())
                .tags(Tag.builder().key("Name").value("web-server-01").build(),
                      Tag.builder().key("Environment").value("production").build())
                .build();

        Reservation reservation = Reservation.builder().instances(instance).build();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().reservations(reservation).build();

        DescribeInstancesIterable mockPaginator = mock(DescribeInstancesIterable.class);
        when(mockPaginator.iterator()).thenReturn(List.of(response).iterator());
        when(ec2Client.describeInstancesPaginator(any(DescribeInstancesRequest.class))).thenReturn(mockPaginator);

        List<Ec2InstanceResource> instances = ec2Provider.describeInstances("us-east-1", "123456789012");

        assertThat(instances).hasSize(1);
        Ec2InstanceResource res = instances.get(0);
        assertThat(res.resourceId()).isEqualTo("i-0123456789abcdef0");
        assertThat(res.resourceType()).isEqualTo(CloudResourceType.EC2_INSTANCE);
        assertThat(res.name()).isEqualTo("web-server-01");
        assertThat(res.status()).isEqualTo("running");
        assertThat(res.privateIp()).isEqualTo("10.0.1.50");
        assertThat(res.publicIp()).isEqualTo("54.210.10.20");
        assertThat(res.tags()).containsEntry("Environment", "production");
    }

    @Test
    @DisplayName("Should successfully inspect deep EC2 instance details with EBS and ENI")
    void shouldGetDeepInstanceDetail() {
        InstanceBlockDeviceMapping bdm = InstanceBlockDeviceMapping.builder()
                .deviceName("/dev/xvda")
                .ebs(EbsInstanceBlockDevice.builder().volumeId("vol-012345").attachTime(Instant.now()).deleteOnTermination(true).build())
                .build();

        InstanceNetworkInterface eni = InstanceNetworkInterface.builder()
                .networkInterfaceId("eni-0123456789")
                .subnetId("subnet-1234")
                .vpcId("vpc-1234")
                .privateIpAddress("10.0.1.10")
                .privateIpAddresses(InstancePrivateIpAddress.builder().privateIpAddress("10.0.1.10").build())
                .groups(GroupIdentifier.builder().groupId("sg-1234").groupName("web-sg").build())
                .macAddress("02:00:00:00:00:01")
                .status("in-use")
                .build();

        Instance instance = Instance.builder()
                .instanceId("i-0123456789abcdef0")
                .instanceType(InstanceType.T3_MICRO)
                .state(InstanceState.builder().name("running").build())
                .privateIpAddress("10.0.1.10")
                .publicIpAddress("54.210.10.20")
                .vpcId("vpc-1234")
                .subnetId("subnet-1234")
                .placement(Placement.builder().availabilityZone("us-east-1a").build())
                .imageId("ami-01234")
                .launchTime(Instant.now())
                .blockDeviceMappings(bdm)
                .networkInterfaces(eni)
                .tags(Tag.builder().key("Name").value("deep-instance").build())
                .build();

        Reservation reservation = Reservation.builder().instances(instance).build();
        DescribeInstancesResponse response = DescribeInstancesResponse.builder().reservations(reservation).build();

        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(response);

        Volume volume = Volume.builder()
                .volumeId("vol-012345")
                .size(50)
                .volumeType(VolumeType.GP3)
                .iops(3000)
                .throughput(125)
                .encrypted(true)
                .state("in-use")
                .availabilityZone("us-east-1a")
                .build();

        when(ec2Client.describeVolumes(any(DescribeVolumesRequest.class)))
                .thenReturn(DescribeVolumesResponse.builder().volumes(volume).build());

        Optional<Ec2DetailResource> opt = ec2Provider.getInstance("i-0123456789abcdef0", "us-east-1", "123456789012");

        assertThat(opt).isPresent();
        Ec2DetailResource detail = opt.get();
        assertThat(detail.instanceId()).isEqualTo("i-0123456789abcdef0");
        assertThat(detail.name()).isEqualTo("deep-instance");
        assertThat(detail.ebsVolumes()).hasSize(1);
        assertThat(detail.ebsVolumes().get(0).volumeId()).isEqualTo("vol-012345");
        assertThat(detail.ebsVolumes().get(0).sizeGiB()).isEqualTo(50);
        assertThat(detail.ebsVolumes().get(0).encrypted()).isTrue();
        assertThat(detail.networkInterfaces()).hasSize(1);
        assertThat(detail.networkInterfaces().get(0).networkInterfaceId()).isEqualTo("eni-0123456789");
        assertThat(detail.networkInterfaces().get(0).securityGroupIds()).contains("sg-1234");
    }

    @Test
    @DisplayName("Should return Optional.empty() when EC2 instance not found")
    void shouldReturnEmptyWhenInstanceNotFound() {
        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("InvalidInstanceID.NotFound")
                .errorMessage("The instance ID 'i-999999' does not exist")
                .build();
        Ec2Exception ec2Exception = (Ec2Exception) Ec2Exception.builder()
                .statusCode(400)
                .awsErrorDetails(errorDetails)
                .build();

        when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenThrow(ec2Exception);

        Optional<Ec2DetailResource> result = ec2Provider.getInstance("i-999999", "us-east-1", "123456789012");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw AwsAccessDeniedException on 403 Forbidden")
    void shouldThrowOn403() {
        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("UnauthorizedOperation")
                .errorMessage("You are not authorized to perform this operation.")
                .build();
        Ec2Exception ec2Exception = (Ec2Exception) Ec2Exception.builder()
                .statusCode(403)
                .awsErrorDetails(errorDetails)
                .build();

        when(ec2Client.describeInstancesPaginator(any(DescribeInstancesRequest.class))).thenThrow(ec2Exception);

        assertThatThrownBy(() -> ec2Provider.describeInstances("us-east-1", "123456789012"))
                .isInstanceOf(AwsAccessDeniedException.class);
    }
}