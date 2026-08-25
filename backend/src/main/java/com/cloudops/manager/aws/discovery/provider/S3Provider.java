package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.S3BucketResource;
import com.cloudops.manager.aws.discovery.model.S3DetailResource;

import java.util.List;
import java.util.Optional;

public interface S3Provider {
    List<S3BucketResource> listBuckets(String region, String accountId);
    Optional<S3DetailResource> getBucket(String bucketName, String region, String accountId);
}