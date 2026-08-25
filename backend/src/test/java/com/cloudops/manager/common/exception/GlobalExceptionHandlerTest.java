package com.cloudops.manager.common.exception;

import com.cloudops.manager.aws.sts.exception.AwsAuthenticationException;
import com.cloudops.manager.common.api.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should sanitize AwsAccessDeniedException to 403 Forbidden with generic message")
    void shouldSanitizeAccessDenied() {
        AwsAccessDeniedException ex = new AwsAccessDeniedException("User arn:aws:iam::123:user/secret is not authorized to perform ec2:DescribeInstances");
        ResponseEntity<ErrorResponse> response = handler.handleAwsAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("AWS_ACCESS_DENIED");
        assertThat(response.getBody().message()).doesNotContain("123", "secret", "DescribeInstances");
        assertThat(response.getBody().message()).isEqualTo("The configured AWS identity does not have permission to perform this operation.");
    }

    @Test
    @DisplayName("Should sanitize AwsAuthenticationException to 401 Unauthorized")
    void shouldSanitizeAuthenticationFailure() {
        AwsAuthenticationException ex = new AwsAuthenticationException("The security token included in the request is expired");
        ResponseEntity<ErrorResponse> response = handler.handleAwsAuthentication(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("AWS_AUTH_FAILED");
        assertThat(response.getBody().message()).isEqualTo("AWS credentials unavailable, expired, or invalid.");
    }

    @Test
    @DisplayName("Should sanitize AwsThrottlingException to 429 Too Many Requests")
    void shouldSanitizeThrottling() {
        AwsThrottlingException ex = new AwsThrottlingException("Request limit exceeded");
        ResponseEntity<ErrorResponse> response = handler.handleAwsThrottling(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("AWS_THROTTLED");
    }

    @Test
    @DisplayName("Should sanitize AwsTimeoutException to 504 Gateway Timeout")
    void shouldSanitizeTimeout() {
        AwsTimeoutException ex = new AwsTimeoutException("Read timed out");
        ResponseEntity<ErrorResponse> response = handler.handleAwsTimeout(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("AWS_TIMEOUT");
    }

    @Test
    @DisplayName("Should sanitize ResourceNotFoundException to 404 Not Found")
    void shouldHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("EC2 instance i-1234567890 not found in region us-east-1");
        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().message()).contains("i-1234567890");
    }
}