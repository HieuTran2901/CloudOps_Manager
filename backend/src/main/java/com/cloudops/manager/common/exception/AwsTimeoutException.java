package com.cloudops.manager.common.exception;

public class AwsTimeoutException extends RuntimeException {
    public AwsTimeoutException(String message) {
        super(message);
    }

    public AwsTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}