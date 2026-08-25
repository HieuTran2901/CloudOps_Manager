package com.cloudops.manager.operations.service;

import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.common.exception.AwsAccessDeniedException;
import com.cloudops.manager.common.exception.AwsThrottlingException;
import com.cloudops.manager.common.exception.AwsTimeoutException;
import com.cloudops.manager.operations.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OperationsMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(OperationsMonitoringService.class);

    private final AwsIdentityService identityService;
    private final OperationalEventBuffer eventBuffer;
    private final String appVersion;
    private final String appRelease;
    private final String defaultRegion;

    private final AtomicReference<Instant> lastSuccessfulSync = new AtomicReference<>(null);
    private final AtomicReference<Instant> lastAttemptedSync = new AtomicReference<>(Instant.now());
    private final AtomicReference<AwsConnectivityStatus> currentStatus = new AtomicReference<>(AwsConnectivityStatus.UNKNOWN);
    private final Map<String, HealthStatus> componentStatusMap = new ConcurrentHashMap<>();

    public OperationsMonitoringService(
            AwsIdentityService identityService,
            OperationalEventBuffer eventBuffer,
            @Value("${cloudops.app.version:1.0.0}") String appVersion,
            @Value("${cloudops.app.release:release-2026.08-p38}") String appRelease,
            @Value("${cloudops.aws.region:us-east-1}") String defaultRegion) {
        this.identityService = identityService;
        this.eventBuffer = eventBuffer;
        this.appVersion = appVersion;
        this.appRelease = appRelease;
        this.defaultRegion = defaultRegion;

        componentStatusMap.put("application", HealthStatus.UP);
        componentStatusMap.put("aws", HealthStatus.UP);
        componentStatusMap.put("discovery", HealthStatus.UP);
        componentStatusMap.put("topology", HealthStatus.UP);
        componentStatusMap.put("security", HealthStatus.UP);
        componentStatusMap.put("forensics", HealthStatus.UP);
        componentStatusMap.put("observability", HealthStatus.UP);
    }

    public DetailedHealthResponse getDetailedHealth() {
        String overallStatus = componentStatusMap.values().stream()
                .anyMatch(s -> s == HealthStatus.UNAVAILABLE) ? "DEGRADED" : "UP";

        return new DetailedHealthResponse(
                overallStatus,
                "cloudops-manager",
                appVersion,
                appRelease,
                Map.copyOf(componentStatusMap),
                Instant.now()
        );
    }

    public AwsOperationalStatus getAwsOperationalStatus(String optionalRegion) {
        String region = (optionalRegion != null && !optionalRegion.isBlank()) ? optionalRegion : defaultRegion;
        lastAttemptedSync.set(Instant.now());

        String accountId = "UNKNOWN";
        AwsConnectivityStatus status;
        String message;

        try {
            var identity = identityService.getCurrentIdentity();
            accountId = identity.accountId();
            status = AwsConnectivityStatus.CONNECTED;
            message = "AWS identity verified successfully.";
            lastSuccessfulSync.set(Instant.now());
            currentStatus.set(status);
        } catch (AwsAccessDeniedException e) {
            status = AwsConnectivityStatus.AWS_ACCESS_DENIED;
            message = "AWS credentials lack required IAM permissions.";
            eventBuffer.recordEvent("AWS_ACCESS_DENIED", OperationalSeverity.WARN, message, "AWS_STS", Map.of("region", region));
            currentStatus.set(status);
        } catch (AwsThrottlingException e) {
            status = AwsConnectivityStatus.AWS_THROTTLED;
            message = "AWS request rate limit exceeded.";
            eventBuffer.recordEvent("AWS_THROTTLED", OperationalSeverity.WARN, message, "AWS_STS", Map.of("region", region));
            currentStatus.set(status);
        } catch (AwsTimeoutException e) {
            status = AwsConnectivityStatus.AWS_TIMEOUT;
            message = "AWS operation timed out.";
            eventBuffer.recordEvent("AWS_TIMEOUT", OperationalSeverity.WARN, message, "AWS_STS", Map.of("region", region));
            currentStatus.set(status);
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (errorMsg.contains("authorized") || errorMsg.contains("access denied") || errorMsg.contains("forbidden")) {
                status = AwsConnectivityStatus.AWS_ACCESS_DENIED;
                message = "AWS credentials lack required IAM permissions.";
                eventBuffer.recordEvent("AWS_ACCESS_DENIED", OperationalSeverity.WARN, message, "AWS_STS", Map.of("region", region));
            } else if (errorMsg.contains("rate exceeded") || errorMsg.contains("throttl")) {
                status = AwsConnectivityStatus.AWS_THROTTLED;
                message = "AWS request rate limit exceeded.";
                eventBuffer.recordEvent("AWS_THROTTLED", OperationalSeverity.WARN, message, "AWS_STS", Map.of("region", region));
            } else if (errorMsg.contains("timeout") || errorMsg.contains("timed out")) {
                status = AwsConnectivityStatus.AWS_TIMEOUT;
                message = "AWS operation timed out.";
                eventBuffer.recordEvent("AWS_TIMEOUT", OperationalSeverity.WARN, message, "AWS_STS", Map.of("region", region));
            } else {
                status = AwsConnectivityStatus.AWS_UNAVAILABLE;
                message = "AWS endpoint currently unreachable or credentials unconfigured.";
                eventBuffer.recordEvent("AWS_UNAVAILABLE", OperationalSeverity.WARN, message, "AWS_STS", Map.of("region", region));
            }
            currentStatus.set(status);
        }

        Instant lastSuccess = lastSuccessfulSync.get();
        Long ageSeconds = (lastSuccess != null) ? Duration.between(lastSuccess, Instant.now()).getSeconds() : null;

        return new AwsOperationalStatus(
                status,
                accountId,
                region,
                lastSuccess,
                lastAttemptedSync.get(),
                ageSeconds,
                message,
                Map.of("version", appVersion, "release", appRelease)
        );
    }

    public void recordSubsystemState(String subsystem, HealthStatus status) {
        if (subsystem != null && status != null) {
            componentStatusMap.put(subsystem.toLowerCase(), status);
        }
    }

    public OperationalEventBuffer getEventBuffer() {
        return eventBuffer;
    }
}
