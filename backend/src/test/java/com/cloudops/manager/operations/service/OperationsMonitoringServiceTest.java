package com.cloudops.manager.operations.service;

import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.common.exception.AwsAccessDeniedException;
import com.cloudops.manager.common.exception.AwsThrottlingException;
import com.cloudops.manager.common.exception.AwsTimeoutException;
import com.cloudops.manager.operations.model.AwsConnectivityStatus;
import com.cloudops.manager.operations.model.AwsOperationalStatus;
import com.cloudops.manager.operations.model.DetailedHealthResponse;
import com.cloudops.manager.operations.model.HealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationsMonitoringServiceTest {

    private AwsIdentityService identityService;
    private OperationalEventBuffer eventBuffer;
    private OperationsMonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        identityService = mock(AwsIdentityService.class);
        eventBuffer = new OperationalEventBuffer();
        monitoringService = new OperationsMonitoringService(
                identityService,
                eventBuffer,
                "1.0.0",
                "release-2026.08-p38",
                "us-east-1"
        );
    }

    @Test
    @DisplayName("Verify healthy AWS identity produces CONNECTED status and records last sync")
    void testHealthyAwsConnection() {
        when(identityService.getCurrentIdentity()).thenReturn(
                new CallerIdentity("111222333444", "arn:aws:iam::111222333444:user/test", "AIDATEST")
        );

        AwsOperationalStatus status = monitoringService.getAwsOperationalStatus("us-east-1");
        assertNotNull(status);
        assertEquals(AwsConnectivityStatus.CONNECTED, status.status());
        assertEquals("111222333444", status.accountId());
        assertNotNull(status.lastSuccessfulSync());
        assertNotNull(status.lastAttemptedSync());
    }

    @Test
    @DisplayName("Verify AccessDenied is mapped to AWS_ACCESS_DENIED without throwing or leaking credentials")
    void testAccessDeniedHandling() {
        when(identityService.getCurrentIdentity()).thenThrow(
                new AwsAccessDeniedException("User arn:aws:iam::123:user/test is not authorized")
        );

        AwsOperationalStatus status = monitoringService.getAwsOperationalStatus("us-east-1");
        assertNotNull(status);
        assertEquals(AwsConnectivityStatus.AWS_ACCESS_DENIED, status.status());
        assertTrue(status.message().contains("IAM permissions"));
    }

    @Test
    @DisplayName("Verify Throttling is mapped to AWS_THROTTLED")
    void testThrottlingHandling() {
        when(identityService.getCurrentIdentity()).thenThrow(
                new AwsThrottlingException("Rate exceeded")
        );

        AwsOperationalStatus status = monitoringService.getAwsOperationalStatus("us-east-1");
        assertNotNull(status);
        assertEquals(AwsConnectivityStatus.AWS_THROTTLED, status.status());
    }

    @Test
    @DisplayName("Verify Timeout is mapped to AWS_TIMEOUT")
    void testTimeoutHandling() {
        when(identityService.getCurrentIdentity()).thenThrow(
                new AwsTimeoutException("Socket timeout")
        );

        AwsOperationalStatus status = monitoringService.getAwsOperationalStatus("us-east-1");
        assertNotNull(status);
        assertEquals(AwsConnectivityStatus.AWS_TIMEOUT, status.status());
    }

    @Test
    @DisplayName("Verify DetailedHealth reports all components and version metadata")
    void testDetailedHealth() {
        DetailedHealthResponse health = monitoringService.getDetailedHealth();
        assertNotNull(health);
        assertEquals("UP", health.status());
        assertEquals("1.0.0", health.version());
        assertEquals("release-2026.08-p38", health.release());
        assertTrue(health.components().containsKey("application"));
        assertTrue(health.components().containsKey("aws"));
        assertTrue(health.components().containsKey("discovery"));
        assertTrue(health.components().containsKey("topology"));
        assertTrue(health.components().containsKey("security"));
        assertTrue(health.components().containsKey("forensics"));

        monitoringService.recordSubsystemState("discovery", HealthStatus.UNAVAILABLE);
        DetailedHealthResponse degradedHealth = monitoringService.getDetailedHealth();
        assertEquals("DEGRADED", degradedHealth.status());
    }
}
