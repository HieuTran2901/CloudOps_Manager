package com.cloudops.manager.common.exception;

public class AwsAccessDeniedException extends RuntimeException {
    public AwsAccessDeniedException(String message) {
        super(message);
    }

    public AwsAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}