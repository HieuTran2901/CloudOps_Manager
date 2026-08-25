package com.cloudops.manager.aws.sts.provider;

import com.cloudops.manager.aws.sts.model.AssumeRoleRequest;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.model.CallerIdentity;

public interface StsIdentityProvider {

    CallerIdentity getCallerIdentity();

    AssumedRoleSession assumeRole(AssumeRoleRequest request);
}