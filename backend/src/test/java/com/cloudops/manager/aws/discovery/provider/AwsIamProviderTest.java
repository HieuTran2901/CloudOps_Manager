package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.IamPolicyDetailResource;
import com.cloudops.manager.aws.discovery.model.IamRoleDetailResource;
import com.cloudops.manager.aws.discovery.model.IamRoleResource;
import com.cloudops.manager.aws.discovery.model.IamUserDetailResource;
import com.cloudops.manager.aws.discovery.model.IamUserResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;
import software.amazon.awssdk.services.iam.paginators.ListRolesIterable;
import software.amazon.awssdk.services.iam.paginators.ListUsersIterable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsIamProviderTest {

    @Mock
    private IamClient iamClient;

    private AwsIamProvider iamProvider;

    @BeforeEach
    void setUp() {
        iamProvider = new AwsIamProvider(iamClient);
    }

    @Test
    @DisplayName("Should list IAM users via paginator")
    void shouldListUsers() {
        User u = User.builder().userName("test-user").userId("AIDATEST").arn("arn:aws:iam::123456789012:user/test-user").path("/").createDate(Instant.now()).build();
        ListUsersIterable paginator = mock(ListUsersIterable.class);
        when(paginator.iterator()).thenReturn(List.of(ListUsersResponse.builder().users(u).build()).iterator());
        when(iamClient.listUsersPaginator(any(ListUsersRequest.class))).thenReturn(paginator);

        List<IamUserResource> users = iamProvider.listUsers("123456789012");

        assertThat(users).hasSize(1);
        assertThat(users.get(0).userName()).isEqualTo("test-user");
        assertThat(users.get(0).accountId()).isEqualTo("123456789012");
    }

    @Test
    @DisplayName("Should inspect IAM user detail with MFA, access keys, groups, and policies")
    void shouldInspectUserDetail() {
        User u = User.builder().userName("alice").userId("AIDAALICE").arn("arn:aws:iam::123456789012:user/alice").path("/").createDate(Instant.now()).build();
        when(iamClient.getUser(any(GetUserRequest.class))).thenReturn(GetUserResponse.builder().user(u).build());

        MFADevice mfa = MFADevice.builder().userName("alice").serialNumber("arn:aws:iam::123456789012:mfa/alice").enableDate(Instant.now()).build();
        when(iamClient.listMFADevices(any(ListMfaDevicesRequest.class))).thenReturn(ListMfaDevicesResponse.builder().mfaDevices(mfa).build());

        AccessKeyMetadata key = AccessKeyMetadata.builder().accessKeyId("AKIAIOSFODNN7EXAMPLE").status(StatusType.ACTIVE).createDate(Instant.now()).build();
        when(iamClient.listAccessKeys(any(ListAccessKeysRequest.class))).thenReturn(ListAccessKeysResponse.builder().accessKeyMetadata(key).build());
        when(iamClient.getAccessKeyLastUsed(any(GetAccessKeyLastUsedRequest.class))).thenReturn(GetAccessKeyLastUsedResponse.builder()
                .accessKeyLastUsed(AccessKeyLastUsed.builder().serviceName("s3").region("us-east-1").lastUsedDate(Instant.now()).build()).build());

        when(iamClient.listGroupsForUser(any(ListGroupsForUserRequest.class))).thenReturn(ListGroupsForUserResponse.builder().groups(Group.builder().groupName("Admins").build()).build());
        when(iamClient.listAttachedUserPolicies(any(ListAttachedUserPoliciesRequest.class))).thenReturn(ListAttachedUserPoliciesResponse.builder().attachedPolicies(AttachedPolicy.builder().policyName("ReadOnlyAccess").policyArn("arn:aws:iam::aws:policy/ReadOnlyAccess").build()).build());
        when(iamClient.listUserPolicies(any(ListUserPoliciesRequest.class))).thenReturn(ListUserPoliciesResponse.builder().policyNames("InlineS3").build());

        Optional<IamUserDetailResource> detail = iamProvider.getUser("alice", "123456789012");

        assertThat(detail).isPresent();
        IamUserDetailResource res = detail.get();
        assertThat(res.userName()).isEqualTo("alice");
        assertThat(res.mfaEnabled()).isTrue();
        assertThat(res.mfaDevices()).hasSize(1);
        assertThat(res.accessKeys()).hasSize(1);
        assertThat(res.accessKeys().get(0).lastUsedServiceName()).isEqualTo("s3");
        assertThat(res.groupNames()).contains("Admins");
        assertThat(res.attachedPolicies()).hasSize(1);
        assertThat(res.inlinePolicyNames()).contains("InlineS3");
    }

    @Test
    @DisplayName("Should inspect IAM role detail with parsed trust policy and attached policies")
    void shouldInspectRoleDetail() {
        String trustDoc = "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"ec2.amazonaws.com\"},\"Action\":\"sts:AssumeRole\"}]}";
        Role role = Role.builder().roleName("EC2AppRole").roleId("AROATEST").arn("arn:aws:iam::123456789012:role/EC2AppRole").assumeRolePolicyDocument(trustDoc).createDate(Instant.now()).build();
        when(iamClient.getRole(any(GetRoleRequest.class))).thenReturn(GetRoleResponse.builder().role(role).build());

        when(iamClient.listAttachedRolePolicies(any(ListAttachedRolePoliciesRequest.class))).thenReturn(ListAttachedRolePoliciesResponse.builder().attachedPolicies(AttachedPolicy.builder().policyName("AmazonS3ReadOnlyAccess").policyArn("arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess").build()).build());
        when(iamClient.listRolePolicies(any(ListRolePoliciesRequest.class))).thenReturn(ListRolePoliciesResponse.builder().build());
        when(iamClient.listInstanceProfilesForRole(any(ListInstanceProfilesForRoleRequest.class))).thenReturn(ListInstanceProfilesForRoleResponse.builder().instanceProfiles(InstanceProfile.builder().instanceProfileName("EC2AppProfile").instanceProfileId("AIPATEST").arn("arn:aws:iam::123456789012:instance-profile/EC2AppProfile").createDate(Instant.now()).build()).build());

        Optional<IamRoleDetailResource> detail = iamProvider.getRole("EC2AppRole", "123456789012");

        assertThat(detail).isPresent();
        IamRoleDetailResource res = detail.get();
        assertThat(res.roleName()).isEqualTo("EC2AppRole");
        assertThat(res.trustPolicyStatements()).hasSize(1);
        assertThat(res.trustPolicyStatements().get(0).principals()).contains("Service:ec2.amazonaws.com");
        assertThat(res.instanceProfiles()).hasSize(1);
        assertThat(res.instanceProfiles().get(0).instanceProfileName()).isEqualTo("EC2AppProfile");
    }

    @Test
    @DisplayName("Should inspect IAM managed policy with parsed statements")
    void shouldInspectPolicy() {
        Policy policy = Policy.builder().arn("arn:aws:iam::123456789012:policy/CustomPolicy").policyName("CustomPolicy").policyId("ANPATYPE").defaultVersionId("v1").isAttachable(true).attachmentCount(2).createDate(Instant.now()).updateDate(Instant.now()).build();
        when(iamClient.getPolicy(any(GetPolicyRequest.class))).thenReturn(GetPolicyResponse.builder().policy(policy).build());

        String doc = "%7B%22Version%22%3A%222012-10-17%22%2C%22Statement%22%3A%5B%7B%22Effect%22%3A%22Allow%22%2C%22Action%22%3A%5B%22s3%3AGetObject%22%2C%22s3%3AListBucket%22%5D%2C%22Resource%22%3A%22%2A%22%7D%5D%7D";
        when(iamClient.getPolicyVersion(any(GetPolicyVersionRequest.class))).thenReturn(GetPolicyVersionResponse.builder().policyVersion(PolicyVersion.builder().document(doc).build()).build());

        Optional<IamPolicyDetailResource> res = iamProvider.getPolicy("arn:aws:iam::123456789012:policy/CustomPolicy", "123456789012");

        assertThat(res).isPresent();
        IamPolicyDetailResource p = res.get();
        assertThat(p.policyName()).isEqualTo("CustomPolicy");
        assertThat(p.policyType()).isEqualTo("CUSTOMER_MANAGED");
        assertThat(p.statements()).hasSize(1);
        assertThat(p.statements().get(0).actions()).contains("s3:GetObject", "s3:ListBucket");
    }
}