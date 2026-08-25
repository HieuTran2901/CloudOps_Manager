package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.IamInstanceProfileInfo;
import com.cloudops.manager.aws.discovery.model.IamPolicyDetailResource;
import com.cloudops.manager.aws.discovery.model.IamRoleDetailResource;
import com.cloudops.manager.aws.discovery.model.IamRoleResource;
import com.cloudops.manager.aws.discovery.model.IamUserDetailResource;
import com.cloudops.manager.aws.discovery.model.IamUserResource;

import java.util.List;
import java.util.Optional;

public interface IamProvider {
    List<IamUserResource> listUsers(String accountId);
    Optional<IamUserDetailResource> getUser(String userName, String accountId);
    List<IamRoleResource> listRoles(String accountId);
    Optional<IamRoleDetailResource> getRole(String roleName, String accountId);
    List<IamInstanceProfileInfo> getInstanceProfilesForRole(String roleName, String accountId);
    Optional<IamPolicyDetailResource> getPolicy(String policyArn, String accountId);
}