package com.cloudops.manager.common.exception;

public class AwsThrottlingException extends RuntimeException {
    public AwsThrottlingException(String message) {
        super(message);
    }

    public AwsThrottlingException(String message, Throwable cause) {
        super(message, cause);
    }
}