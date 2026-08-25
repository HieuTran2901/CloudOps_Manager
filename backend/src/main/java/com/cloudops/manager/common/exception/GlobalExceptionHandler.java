package com.cloudops.manager.common.exception;

import com.cloudops.manager.aws.sts.exception.AwsAuthenticationException;
import com.cloudops.manager.aws.sts.exception.AwsIdentityException;
import com.cloudops.manager.common.api.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AwsAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAwsAccessDenied(AwsAccessDeniedException ex) {
        log.warn("AWS AccessDenied encountered: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("AWS_ACCESS_DENIED", "The configured AWS identity does not have permission to perform this operation."));
    }

    @ExceptionHandler(AwsAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAwsAuthentication(AwsAuthenticationException ex) {
        log.warn("AWS Authentication failure: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("AWS_AUTH_FAILED", "AWS credentials unavailable, expired, or invalid."));
    }

    @ExceptionHandler(AwsThrottlingException.class)
    public ResponseEntity<ErrorResponse> handleAwsThrottling(AwsThrottlingException ex) {
        log.warn("AWS Throttling encountered: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorResponse.of("AWS_THROTTLED", "AWS request rate limit exceeded. Please retry later."));
    }

    @ExceptionHandler(AwsTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleAwsTimeout(AwsTimeoutException ex) {
        log.error("AWS Timeout: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                .body(ErrorResponse.of("AWS_TIMEOUT", "AWS operation timed out. Please retry later."));
    }

    @ExceptionHandler(AwsServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleAwsServiceUnavailable(AwsServiceUnavailableException ex) {
        log.error("AWS Service Unavailable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of("AWS_SERVICE_UNAVAILABLE", "AWS service is temporarily unreachable."));
    }

    @ExceptionHandler(AwsIdentityException.class)
    public ResponseEntity<ErrorResponse> handleAwsIdentity(AwsIdentityException ex) {
        log.error("AWS Identity resolution error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of("AWS_IDENTITY_ERROR", "Failed to resolve AWS identity or connect to AWS STS."));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.info("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("INVALID_ARGUMENT", "Invalid request parameters provided: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled internal server error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "An unexpected error occurred."));
    }
}