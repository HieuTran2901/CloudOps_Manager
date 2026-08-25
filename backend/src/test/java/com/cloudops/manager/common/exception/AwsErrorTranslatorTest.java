package com.cloudops.manager.common.exception;

import com.cloudops.manager.aws.sts.exception.AwsAuthenticationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class AwsErrorTranslatorTest {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(AwsErrorTranslatorTest.class);

    @Test
    @DisplayName("Should translate 403 to AwsAccessDeniedException")
    void shouldTranslate403() {
        AwsServiceException ex = (AwsServiceException) AwsServiceException.builder()
                .statusCode(403)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("UnauthorizedOperation").errorMessage("Denied").build())
                .build();

        RuntimeException result = AwsErrorTranslator.translate("EC2:Describe", ex, log);
        assertThat(result).isInstanceOf(AwsAccessDeniedException.class);
    }

    @Test
    @DisplayName("Should translate ExpiredToken to AwsAuthenticationException")
    void shouldTranslateExpiredToken() {
        AwsServiceException ex = (AwsServiceException) AwsServiceException.builder()
                .statusCode(400)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("ExpiredToken").errorMessage("Token expired").build())
                .build();

        RuntimeException result = AwsErrorTranslator.translate("STS:GetCallerIdentity", ex, log);
        assertThat(result).isInstanceOf(AwsAuthenticationException.class);
    }

    @Test
    @DisplayName("Should translate Throttling to AwsThrottlingException")
    void shouldTranslateThrottling() {
        AwsServiceException ex = (AwsServiceException) AwsServiceException.builder()
                .statusCode(429)
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("RequestLimitExceeded").errorMessage("Slow down").build())
                .build();

        RuntimeException result = AwsErrorTranslator.translate("EC2:DescribeInstances", ex, log);
        assertThat(result).isInstanceOf(AwsThrottlingException.class);
    }

    @Test
    @DisplayName("Should translate ApiCallTimeoutException to AwsTimeoutException")
    void shouldTranslateTimeout() {
        ApiCallTimeoutException ex = (ApiCallTimeoutException) ApiCallTimeoutException.builder()
                .message("Timeout exceeded")
                .build();

        RuntimeException result = AwsErrorTranslator.translate("RDS:Describe", ex, log);
        assertThat(result).isInstanceOf(AwsTimeoutException.class);
    }
}