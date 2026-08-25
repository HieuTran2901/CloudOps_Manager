package com.cloudops.manager.aws.sts.exception;

public class AwsIdentityException extends RuntimeException {
    public AwsIdentityException(String message) {
        super(message);
    }

    public AwsIdentityException(String message, Throwable cause) {
        super(message, cause);
    }
}