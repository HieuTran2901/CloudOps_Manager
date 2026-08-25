package com.cloudops.manager.aws.discovery.model;

import java.time.Instant;

public record IamMfaDeviceInfo(
    String serialNumber,
    Instant enableDate
) {}