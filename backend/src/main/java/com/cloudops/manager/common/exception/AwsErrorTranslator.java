package com.cloudops.manager.common.exception;

import com.cloudops.manager.aws.sts.exception.AwsAuthenticationException;
import com.cloudops.manager.aws.sts.exception.AwsIdentityException;
import org.slf4j.Logger;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkClientException;

public final class AwsErrorTranslator {

    private AwsErrorTranslator() {}

    public static RuntimeException translate(String operation, Exception ex, Logger log) {
        if (ex instanceof AwsServiceException awsEx) {
            String errorCode = awsEx.awsErrorDetails() != null ? awsEx.awsErrorDetails().errorCode() : "UNKNOWN";
            String rawMessage = awsEx.awsErrorDetails() != null ? awsEx.awsErrorDetails().errorMessage() : awsEx.getMessage();
            int statusCode = awsEx.statusCode();

            log.error("AWS Error during [{}] - Status: {}, ErrorCode: {}, Detail: {}", operation, statusCode, errorCode, rawMessage);

            if (statusCode == 403 || "AccessDenied".equalsIgnoreCase(errorCode)
                    || "UnauthorizedOperation".equalsIgnoreCase(errorCode)
                    || "AuthFailure".equalsIgnoreCase(errorCode)) {
                return new AwsAccessDeniedException("AWS Access Denied during " + operation, awsEx);
            }
            if (statusCode == 401 || "InvalidClientTokenId".equalsIgnoreCase(errorCode)
                    || "ExpiredToken".equalsIgnoreCase(errorCode)
                    || "SignatureDoesNotMatch".equalsIgnoreCase(errorCode)) {
                return new AwsAuthenticationException("AWS Authentication failure during " + operation, awsEx);
            }
            if (statusCode == 429 || "Throttling".equalsIgnoreCase(errorCode)
                    || "ThrottlingException".equalsIgnoreCase(errorCode)
                    || "RequestLimitExceeded".equalsIgnoreCase(errorCode)
                    || "ProvisionedThroughputExceededException".equalsIgnoreCase(errorCode)) {
                return new AwsThrottlingException("AWS Throttling during " + operation, awsEx);
            }
            if (statusCode >= 500) {
                return new AwsServiceUnavailableException("AWS Service Unavailable during " + operation, awsEx);
            }
            return new AwsIdentityException("AWS Service Error during " + operation + ": " + errorCode, awsEx);
        }

        if (ex instanceof ApiCallTimeoutException timeoutEx) {
            log.error("AWS Timeout during [{}]: {}", operation, timeoutEx.getMessage());
            return new AwsTimeoutException("AWS Timeout during " + operation, timeoutEx);
        }

        if (ex instanceof SdkClientException clientEx) {
            log.error("AWS SDK Client error during [{}]: {}", operation, clientEx.getMessage());
            if (clientEx.getMessage() != null && clientEx.getMessage().toLowerCase().contains("credential")) {
                return new AwsAuthenticationException("AWS Client Credentials error during " + operation, clientEx);
            }
            return new AwsServiceUnavailableException("AWS Client connection error during " + operation, clientEx);
        }

        log.error("Unexpected error during [{}]: {}", operation, ex.getMessage(), ex);
        return new AwsIdentityException("Unexpected error during " + operation, ex);
    }
}