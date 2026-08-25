package com.cloudops.manager.aws.sts.exception;

public class AwsAuthenticationException extends RuntimeException {
    public AwsAuthenticationException(String message) {
        super(message);
    }

    public AwsAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}