package com.cloudops.manager.common.exception;

public class AwsServiceUnavailableException extends RuntimeException {
    public AwsServiceUnavailableException(String message) {
        super(message);
    }

    public AwsServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}