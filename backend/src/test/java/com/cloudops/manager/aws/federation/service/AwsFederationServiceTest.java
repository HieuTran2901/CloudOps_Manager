package com.cloudops.manager.aws.federation.service;

import com.cloudops.manager.aws.federation.model.*;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.common.exception.AwsAccessDeniedException;
import com.cloudops.manager.common.exception.AwsThrottlingException;
import com.cloudops.manager.common.exception.AwsTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AwsFederationServiceTest {

    private AwsIdentityService identityService;
    private AwsFederationService federationService;

    @BeforeEach
    void setUp() {
        identityService = mock(AwsIdentityService.class);
        when(identityService.getCurrentIdentity()).thenReturn(
                new CallerIdentity("111222333444", "arn:aws:iam::111222333444:user/root", "AIDAROOT")
        );
        federationService = new AwsFederationService(identityService, "us-east-1");
    }

    @Test
    @DisplayName("Valid federation request succeeds with status FEDERATED")
    void testSuccessfulFederation() {
        AssumedRoleSession mockSession = new AssumedRoleSession(
                "ASIAEXAMPLEKEY",
                "secret-key",
                "session-token",
                Instant.now().plusSeconds(3600),
                "arn:aws:sts::555666777888:assumed-role/CloudOpsRole/session"
        );
        when(identityService.assumeRole(any())).thenReturn(mockSession);

        FederationRequest req = new FederationRequest(
                "555666777888",
                "arn:aws:iam::555666777888:role/CloudOpsRole",
                "test-session",
                "us-east-1",
                null
        );

        FederationResult result = federationService.federateAccount(req);
        assertNotNull(result);
        assertEquals(FederationStatus.FEDERATED, result.status());
        assertEquals("555666777888", result.targetAccountId());
        assertEquals("arn:aws:iam::555666777888:role/CloudOpsRole", result.assumedRoleArn());
    }

    @Test
    @DisplayName("Invalid Role ARN or account ID mismatch returns INVALID_ROLE")
    void testInvalidRoleArnMismatch() {
        FederationRequest req = new FederationRequest(
                "123456789012",
                "arn:aws:iam::999999999999:role/OtherRole",
                "test-session",
                "us-east-1",
                null
        );

        FederationResult result = federationService.federateAccount(req);
        assertNotNull(result);
        assertEquals(FederationStatus.INVALID_ROLE, result.status());
        assertTrue(result.message().contains("does not match target account ID"));
    }

    @Test
    @DisplayName("AccessDenied during assumeRole returns ACCESS_DENIED")
    void testAccessDeniedHandling() {
        when(identityService.assumeRole(any())).thenThrow(
                new AwsAccessDeniedException("AccessDenied: User not authorized to perform sts:AssumeRole")
        );

        FederationRequest req = new FederationRequest(
                "555666777888",
                "arn:aws:iam::555666777888:role/CloudOpsRole",
                "test-session",
                "us-east-1",
                null
        );

        FederationResult result = federationService.federateAccount(req);
        assertNotNull(result);
        assertEquals(FederationStatus.ACCESS_DENIED, result.status());
    }

    @Test
    @DisplayName("Throttling during assumeRole returns AWS_THROTTLED")
    void testThrottlingHandling() {
        when(identityService.assumeRole(any())).thenThrow(
                new AwsThrottlingException("Rate exceeded")
        );

        FederationRequest req = new FederationRequest(
                "555666777888",
                "arn:aws:iam::555666777888:role/CloudOpsRole",
                "test-session",
                "us-east-1",
                null
        );

        FederationResult result = federationService.federateAccount(req);
        assertNotNull(result);
        assertEquals(FederationStatus.AWS_THROTTLED, result.status());
    }

    @Test
    @DisplayName("Timeout during assumeRole returns AWS_TIMEOUT")
    void testTimeoutHandling() {
        when(identityService.assumeRole(any())).thenThrow(
                new AwsTimeoutException("Socket timeout")
        );

        FederationRequest req = new FederationRequest(
                "555666777888",
                "arn:aws:iam::555666777888:role/CloudOpsRole",
                "test-session",
                "us-east-1",
                null
        );

        FederationResult result = federationService.federateAccount(req);
        assertNotNull(result);
        assertEquals(FederationStatus.AWS_TIMEOUT, result.status());
    }

    @Test
    @DisplayName("Assumed role session account mismatch returns ACCOUNT_MISMATCH")
    void testAccountMismatchDetection() {
        AssumedRoleSession mockSession = new AssumedRoleSession(
                "ASIAEXAMPLEKEY",
                "secret-key",
                "session-token",
                Instant.now().plusSeconds(3600),
                "arn:aws:sts::999999999999:assumed-role/CloudOpsRole/session" // returned wrong account
        );
        when(identityService.assumeRole(any())).thenReturn(mockSession);

        FederationRequest req = new FederationRequest(
                "555666777888",
                "arn:aws:iam::555666777888:role/CloudOpsRole",
                "test-session",
                "us-east-1",
                null
        );

        FederationResult result = federationService.federateAccount(req);
        assertNotNull(result);
        assertEquals(FederationStatus.ACCOUNT_MISMATCH, result.status());
    }
}