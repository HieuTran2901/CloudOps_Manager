package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.common.exception.AwsErrorTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.iam.model.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AwsIamProvider implements IamProvider {

    private static final Logger log = LoggerFactory.getLogger(AwsIamProvider.class);
    private final IamClient iamClient;

    public AwsIamProvider(IamClient iamClient) {
        this.iamClient = iamClient;
    }

    @Override
    public List<IamUserResource> listUsers(String accountId) {
        log.info("Listing IAM users for account: {}", accountId);
        List<IamUserResource> users = new ArrayList<>();
        try {
            var paginator = iamClient.listUsersPaginator(ListUsersRequest.builder().build());
            for (var page : paginator) {
                for (User u : page.users()) {
                    users.add(new IamUserResource(u.userName(), u.userId(), u.arn(), u.path(), u.createDate(), accountId));
                }
            }
            log.info("Discovered {} IAM users", users.size());
            return users;
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("IAM:ListUsers", e, log);
        }
    }

    @Override
    public Optional<IamUserDetailResource> getUser(String userName, String accountId) {
        log.info("Inspecting IAM user: {} for account: {}", userName, accountId);
        try {
            User user = iamClient.getUser(GetUserRequest.builder().userName(userName).build()).user();
            List<IamMfaDeviceInfo> mfas = queryMfaDevices(userName);
            List<IamAccessKeyMetadata> keys = queryAccessKeys(userName);
            List<String> groups = queryUserGroups(userName);
            List<IamPolicyAttachmentInfo> attached = queryUserAttachedPolicies(userName);
            List<String> inline = queryUserInlinePolicies(userName);
            Map<String, String> tags = user.hasTags() ? user.tags().stream().collect(Collectors.toMap(Tag::key, Tag::value, (k1, k2) -> k1)) : Collections.emptyMap();

            return Optional.of(new IamUserDetailResource(
                    user.userName(), user.userId(), user.arn(), user.path(), user.createDate(), accountId,
                    !mfas.isEmpty(), mfas, keys, groups, attached, inline, tags, Instant.now()
            ));
        } catch (NoSuchEntityException e) {
            return Optional.empty();
        } catch (IamException e) {
            if (e.statusCode() == 404) return Optional.empty();
            throw AwsErrorTranslator.translate("IAM:GetUser:" + userName, e, log);
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("IAM:GetUser:" + userName, e, log);
        }
    }

    @Override
    public List<IamRoleResource> listRoles(String accountId) {
        log.info("Listing IAM roles for account: {}", accountId);
        List<IamRoleResource> roles = new ArrayList<>();
        try {
            var paginator = iamClient.listRolesPaginator(ListRolesRequest.builder().build());
            for (var page : paginator) {
                for (Role r : page.roles()) {
                    roles.add(new IamRoleResource(r.roleName(), r.roleId(), r.arn(), r.path(), r.createDate(), accountId));
                }
            }
            log.info("Discovered {} IAM roles", roles.size());
            return roles;
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("IAM:ListRoles", e, log);
        }
    }

    @Override
    public Optional<IamRoleDetailResource> getRole(String roleName, String accountId) {
        log.info("Inspecting IAM role: {} for account: {}", roleName, accountId);
        try {
            Role role = iamClient.getRole(GetRoleRequest.builder().roleName(roleName).build()).role();
            List<IamTrustStatement> trust = IamPolicyDocumentParser.parseTrustStatements(role.assumeRolePolicyDocument());
            List<IamPolicyAttachmentInfo> attached = queryRoleAttachedPolicies(roleName);
            List<String> inline = queryRoleInlinePolicies(roleName);
            List<IamInstanceProfileInfo> profiles = getInstanceProfilesForRole(roleName, accountId);
            Map<String, String> tags = role.hasTags() ? role.tags().stream().collect(Collectors.toMap(Tag::key, Tag::value, (k1, k2) -> k1)) : Collections.emptyMap();

            return Optional.of(new IamRoleDetailResource(
                    role.roleName(), role.roleId(), role.arn(), role.path(), role.createDate(), accountId,
                    role.description(), role.maxSessionDuration(), trust, attached, inline, profiles, tags, Instant.now()
            ));
        } catch (NoSuchEntityException e) {
            return Optional.empty();
        } catch (IamException e) {
            if (e.statusCode() == 404) return Optional.empty();
            throw AwsErrorTranslator.translate("IAM:GetRole:" + roleName, e, log);
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("IAM:GetRole:" + roleName, e, log);
        }
    }

    @Override
    public List<IamInstanceProfileInfo> getInstanceProfilesForRole(String roleName, String accountId) {
        try {
            var resp = iamClient.listInstanceProfilesForRole(ListInstanceProfilesForRoleRequest.builder().roleName(roleName).build());
            return resp.hasInstanceProfiles() ? resp.instanceProfiles().stream()
                    .map(ip -> new IamInstanceProfileInfo(ip.instanceProfileName(), ip.instanceProfileId(), ip.arn(), ip.path(), ip.createDate()))
                    .toList() : List.of();
        } catch (Exception e) {
            log.debug("Instance profiles lookup error for role {}: {}", roleName, e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<IamPolicyDetailResource> getPolicy(String policyArn, String accountId) {
        log.info("Inspecting IAM policy: {}", policyArn);
        try {
            Policy policy = iamClient.getPolicy(GetPolicyRequest.builder().policyArn(policyArn).build()).policy();
            var verResp = iamClient.getPolicyVersion(GetPolicyVersionRequest.builder().policyArn(policyArn).versionId(policy.defaultVersionId()).build());
            List<IamPolicyStatement> stmts = IamPolicyDocumentParser.parsePolicyStatements(verResp.policyVersion().document());
            String type = policyArn.startsWith("arn:aws:iam::aws:policy") ? "AWS_MANAGED" : "CUSTOMER_MANAGED";

            return Optional.of(new IamPolicyDetailResource(
                    policy.arn(), policy.policyName(), policy.policyId(), policy.path(), policy.isAttachable(),
                    policy.attachmentCount(), policy.defaultVersionId(), policy.createDate(), policy.updateDate(),
                    type, stmts, Instant.now()
            ));
        } catch (NoSuchEntityException e) {
            return Optional.empty();
        } catch (IamException e) {
            if (e.statusCode() == 404) return Optional.empty();
            throw AwsErrorTranslator.translate("IAM:GetPolicy:" + policyArn, e, log);
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("IAM:GetPolicy:" + policyArn, e, log);
        }
    }

    private List<IamMfaDeviceInfo> queryMfaDevices(String userName) {
        try {
            var resp = iamClient.listMFADevices(ListMfaDevicesRequest.builder().userName(userName).build());
            return resp.hasMfaDevices() ? resp.mfaDevices().stream().map(m -> new IamMfaDeviceInfo(m.serialNumber(), m.enableDate())).toList() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<IamAccessKeyMetadata> queryAccessKeys(String userName) {
        try {
            var resp = iamClient.listAccessKeys(ListAccessKeysRequest.builder().userName(userName).build());
            if (!resp.hasAccessKeyMetadata()) return List.of();
            return resp.accessKeyMetadata().stream().map(k -> {
                Instant lastUsed = null;
                String service = null;
                String region = null;
                try {
                    var u = iamClient.getAccessKeyLastUsed(GetAccessKeyLastUsedRequest.builder().accessKeyId(k.accessKeyId()).build()).accessKeyLastUsed();
                    if (u != null) {
                        lastUsed = u.lastUsedDate();
                        service = u.serviceName();
                        region = u.region();
                    }
                } catch (Exception ignored) {}
                return new IamAccessKeyMetadata(k.accessKeyId(), k.statusAsString(), k.createDate(), lastUsed, service, region);
            }).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> queryUserGroups(String userName) {
        try {
            var resp = iamClient.listGroupsForUser(ListGroupsForUserRequest.builder().userName(userName).build());
            return resp.hasGroups() ? resp.groups().stream().map(Group::groupName).toList() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<IamPolicyAttachmentInfo> queryUserAttachedPolicies(String userName) {
        try {
            var resp = iamClient.listAttachedUserPolicies(ListAttachedUserPoliciesRequest.builder().userName(userName).build());
            return resp.hasAttachedPolicies() ? resp.attachedPolicies().stream().map(p -> new IamPolicyAttachmentInfo(p.policyName(), p.policyArn())).toList() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> queryUserInlinePolicies(String userName) {
        try {
            var resp = iamClient.listUserPolicies(ListUserPoliciesRequest.builder().userName(userName).build());
            return resp.hasPolicyNames() ? resp.policyNames() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<IamPolicyAttachmentInfo> queryRoleAttachedPolicies(String roleName) {
        try {
            var resp = iamClient.listAttachedRolePolicies(ListAttachedRolePoliciesRequest.builder().roleName(roleName).build());
            return resp.hasAttachedPolicies() ? resp.attachedPolicies().stream().map(p -> new IamPolicyAttachmentInfo(p.policyName(), p.policyArn())).toList() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<String> queryRoleInlinePolicies(String roleName) {
        try {
            var resp = iamClient.listRolePolicies(ListRolePoliciesRequest.builder().roleName(roleName).build());
            return resp.hasPolicyNames() ? resp.policyNames() : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
}