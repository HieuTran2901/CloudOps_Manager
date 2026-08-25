package com.cloudops.manager.aws.sts.service;

import com.cloudops.manager.aws.sts.model.AssumeRoleRequest;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.provider.StsIdentityProvider;
import org.springframework.stereotype.Service;

@Service
public class AwsIdentityService {

    private final StsIdentityProvider stsIdentityProvider;

    public AwsIdentityService(StsIdentityProvider stsIdentityProvider) {
        this.stsIdentityProvider = stsIdentityProvider;
    }

    public CallerIdentity getCurrentIdentity() {
        return stsIdentityProvider.getCallerIdentity();
    }

    public AssumedRoleSession assumeRole(AssumeRoleRequest request) {
        return stsIdentityProvider.assumeRole(request);
    }
}