package com.cloudops.manager.aws.forensics.model;

public record ForensicExportResult(
    ForensicMetadata metadata,
    String content,
    String sha256Digest,
    String contentType,
    String filename
) {}