package com.cloudops.manager.aws.dashboard.model;

import java.time.Instant;

public record SubsystemSnapshot<T>(
        SubsystemStatus status,
        String source,
        T data,
        Instant fetchedAt,
        String errorCode,
        String errorMessage
) {
    public static <T> SubsystemSnapshot<T> live(String source, T data) {
        return new SubsystemSnapshot<>(SubsystemStatus.LIVE, source, data, Instant.now(), null, null);
    }

    public static <T> SubsystemSnapshot<T> empty(String source, T emptyData) {
        return new SubsystemSnapshot<>(SubsystemStatus.EMPTY, source, emptyData, Instant.now(), null, null);
    }

    public static <T> SubsystemSnapshot<T> denied(String source, String errorCode, String errorMessage) {
        return new SubsystemSnapshot<>(SubsystemStatus.DENIED, source, null, Instant.now(), errorCode, errorMessage);
    }

    public static <T> SubsystemSnapshot<T> error(String source, String errorCode, String errorMessage) {
        return new SubsystemSnapshot<>(SubsystemStatus.ERROR, source, null, Instant.now(), errorCode, errorMessage);
    }
}
