package com.cloudops.manager.aws.sts.service;

import com.cloudops.manager.aws.sts.model.AssumeRoleRequest;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.provider.StsIdentityProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsIdentityServiceTest {

    @Mock
    private StsIdentityProvider stsIdentityProvider;

    @InjectMocks
    private AwsIdentityService awsIdentityService;

    @Test
    @DisplayName("Should delegate getCurrentIdentity to StsIdentityProvider")
    void shouldDelegateGetCurrentIdentity() {
        CallerIdentity mockIdentity = new CallerIdentity("112233445566", "arn:aws:iam::112233445566:root", "ROOTID");
        when(stsIdentityProvider.getCallerIdentity()).thenReturn(mockIdentity);

        CallerIdentity result = awsIdentityService.getCurrentIdentity();

        assertThat(result).isEqualTo(mockIdentity);
        verify(stsIdentityProvider).getCallerIdentity();
    }

    @Test
    @DisplayName("Should delegate assumeRole to StsIdentityProvider")
    void shouldDelegateAssumeRole() {
        AssumeRoleRequest request = AssumeRoleRequest.of("arn:aws:iam::112233445566:role/Operator", "session1");
        AssumedRoleSession session = new AssumedRoleSession(
                "ASIAKEY", "SECRET", "TOKEN", Instant.now().plusSeconds(1800), "arn:aws:sts::112233445566:assumed-role/Operator/session1"
        );
        when(stsIdentityProvider.assumeRole(request)).thenReturn(session);

        AssumedRoleSession result = awsIdentityService.assumeRole(request);

        assertThat(result).isEqualTo(session);
        verify(stsIdentityProvider).assumeRole(request);
    }
}