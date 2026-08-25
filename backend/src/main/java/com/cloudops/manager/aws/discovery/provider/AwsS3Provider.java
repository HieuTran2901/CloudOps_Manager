package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.CloudResourceType;
import com.cloudops.manager.aws.discovery.model.S3BucketPolicySummary;
import com.cloudops.manager.aws.discovery.model.S3BucketResource;
import com.cloudops.manager.aws.discovery.model.S3CorsConfiguration;
import com.cloudops.manager.aws.discovery.model.S3CorsRule;
import com.cloudops.manager.aws.discovery.model.S3DetailResource;
import com.cloudops.manager.aws.discovery.model.S3EncryptionConfiguration;
import com.cloudops.manager.aws.discovery.model.S3LifecycleConfiguration;
import com.cloudops.manager.aws.discovery.model.S3LifecycleRule;
import com.cloudops.manager.aws.discovery.model.S3PublicAccessBlock;
import com.cloudops.manager.aws.discovery.model.S3VersioningConfiguration;
import com.cloudops.manager.common.exception.AwsErrorTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.GetBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.GetBucketCorsResponse;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLocationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLocationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingResponse;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionRule;
import software.amazon.awssdk.services.s3.model.Tag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AwsS3Provider implements S3Provider {

    private static final Logger log = LoggerFactory.getLogger(AwsS3Provider.class);
    private final S3Client s3Client;

    public AwsS3Provider(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public List<S3BucketResource> listBuckets(String region, String accountId) {
        log.info("Discovering S3 buckets for account: {}", accountId);
        List<S3BucketResource> results = new ArrayList<>();
        Instant discoveredAt = Instant.now();

        try {
            ListBucketsResponse response = s3Client.listBuckets();
            for (Bucket bucket : response.buckets()) {
                String arn = "arn:aws:s3:::" + bucket.name();
                results.add(new S3BucketResource(
                        bucket.name(), CloudResourceType.S3_BUCKET, bucket.name(),
                        region, accountId, "ACTIVE", arn, Collections.emptyMap(),
                        discoveredAt, bucket.creationDate()
                ));
            }
            log.info("Discovered {} S3 buckets", results.size());
            return results;
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("S3:ListBuckets", e, log);
        }
    }

    @Override
    public Optional<S3DetailResource> getBucket(String bucketName, String region, String accountId) {
        log.info("Inspecting S3 bucket: {} for account: {}", bucketName, accountId);
        try {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            } catch (NoSuchBucketException e) {
                log.info("S3 bucket {} does not exist (NoSuchBucket)", bucketName);
                return Optional.empty();
            } catch (S3Exception e) {
                if (e.statusCode() == 404 || "NoSuchBucket".equalsIgnoreCase(e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "")) {
                    log.info("S3 bucket {} does not exist (404)", bucketName);
                    return Optional.empty();
                }
                if (e.statusCode() == 403) {
                    log.info("HeadBucket returned 403, proceeding to inspect configurations for: {}", bucketName);
                } else {
                    throw e;
                }
            }

            Instant creationDate = null;
            try {
                ListBucketsResponse listResponse = s3Client.listBuckets();
                for (Bucket b : listResponse.buckets()) {
                    if (b.name().equals(bucketName)) {
                        creationDate = b.creationDate();
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("Could not determine creation date from listBuckets for {}: {}", bucketName, e.getMessage());
            }

            String bucketRegion = region;
            try {
                GetBucketLocationResponse locResponse = s3Client.getBucketLocation(
                        GetBucketLocationRequest.builder().bucket(bucketName).build()
                );
                if (locResponse.locationConstraintAsString() != null && !locResponse.locationConstraintAsString().isBlank()) {
                    bucketRegion = locResponse.locationConstraintAsString();
                }
            } catch (Exception e) {
                log.debug("GetBucketLocation returned: {}", e.getMessage());
            }

            S3PublicAccessBlock pab = inspectPublicAccessBlock(bucketName);
            S3EncryptionConfiguration enc = inspectEncryption(bucketName);
            S3VersioningConfiguration ver = inspectVersioning(bucketName);
            S3BucketPolicySummary policy = inspectPolicy(bucketName);
            S3CorsConfiguration cors = inspectCors(bucketName);
            S3LifecycleConfiguration lifecycle = inspectLifecycle(bucketName);
            Map<String, String> tags = inspectTags(bucketName);

            String arn = "arn:aws:s3:::" + bucketName;

            return Optional.of(new S3DetailResource(
                    bucketName, arn, accountId, bucketRegion, creationDate, pab,
                    policy, enc, ver, cors, lifecycle, tags, Instant.now()
            ));
        } catch (NoSuchBucketException e) {
            log.info("S3 bucket {} not found", bucketName);
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404 || "NoSuchBucket".equalsIgnoreCase(e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "")) {
                log.info("S3 bucket {} does not exist (404)", bucketName);
                return Optional.empty();
            }
            throw AwsErrorTranslator.translate("S3:GetBucket:" + bucketName, e, log);
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("S3:GetBucket:" + bucketName, e, log);
        }
    }

    private S3PublicAccessBlock inspectPublicAccessBlock(String bucketName) {
        try {
            GetPublicAccessBlockResponse response = s3Client.getPublicAccessBlock(
                    GetPublicAccessBlockRequest.builder().bucket(bucketName).build()
            );
            PublicAccessBlockConfiguration c = response.publicAccessBlockConfiguration();
            return new S3PublicAccessBlock("CONFIGURED", c.blockPublicAcls(), c.ignorePublicAcls(), c.blockPublicPolicy(), c.restrictPublicBuckets());
        } catch (AwsServiceException e) {
            int status = e.statusCode();
            String code = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "";
            if (status == 404 || "NoSuchPublicAccessBlockConfiguration".equalsIgnoreCase(code)) {
                return new S3PublicAccessBlock("NOT_CONFIGURED", false, false, false, false);
            }
            if (status == 403 || "AccessDenied".equalsIgnoreCase(code)) {
                return new S3PublicAccessBlock("ACCESS_DENIED", null, null, null, null);
            }
            return new S3PublicAccessBlock("UNAVAILABLE", null, null, null, null);
        }
    }

    private S3EncryptionConfiguration inspectEncryption(String bucketName) {
        try {
            GetBucketEncryptionResponse response = s3Client.getBucketEncryption(
                    GetBucketEncryptionRequest.builder().bucket(bucketName).build()
            );
            if (response.serverSideEncryptionConfiguration() != null && !response.serverSideEncryptionConfiguration().rules().isEmpty()) {
                ServerSideEncryptionRule rule = response.serverSideEncryptionConfiguration().rules().get(0);
                String algo = rule.applyServerSideEncryptionByDefault() != null ? rule.applyServerSideEncryptionByDefault().sseAlgorithmAsString() : null;
                String kmsKey = rule.applyServerSideEncryptionByDefault() != null ? rule.applyServerSideEncryptionByDefault().kmsMasterKeyID() : null;
                return new S3EncryptionConfiguration("CONFIGURED", true, algo, kmsKey, rule.bucketKeyEnabled());
            }
            return new S3EncryptionConfiguration("NOT_CONFIGURED", false, null, null, false);
        } catch (AwsServiceException e) {
            int status = e.statusCode();
            String code = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "";
            if (status == 404 || "ServerSideEncryptionConfigurationNotFoundError".equalsIgnoreCase(code)) {
                return new S3EncryptionConfiguration("NOT_CONFIGURED", false, null, null, false);
            }
            if (status == 403 || "AccessDenied".equalsIgnoreCase(code)) {
                return new S3EncryptionConfiguration("ACCESS_DENIED", null, null, null, null);
            }
            return new S3EncryptionConfiguration("UNAVAILABLE", null, null, null, null);
        }
    }

    private S3VersioningConfiguration inspectVersioning(String bucketName) {
        try {
            GetBucketVersioningResponse response = s3Client.getBucketVersioning(
                    GetBucketVersioningRequest.builder().bucket(bucketName).build()
            );
            String status = response.statusAsString() != null ? response.statusAsString() : "NotEnabled";
            String mfa = response.mfaDeleteAsString() != null ? response.mfaDeleteAsString() : "Disabled";
            return new S3VersioningConfiguration("CONFIGURED", status, mfa);
        } catch (AwsServiceException e) {
            int status = e.statusCode();
            String code = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "";
            if (status == 403 || "AccessDenied".equalsIgnoreCase(code)) {
                return new S3VersioningConfiguration("ACCESS_DENIED", null, null);
            }
            return new S3VersioningConfiguration("UNAVAILABLE", null, null);
        }
    }

    private S3BucketPolicySummary inspectPolicy(String bucketName) {
        try {
            GetBucketPolicyResponse response = s3Client.getBucketPolicy(
                    GetBucketPolicyRequest.builder().bucket(bucketName).build()
            );
            return new S3BucketPolicySummary("CONFIGURED", true, response.policy(), null);
        } catch (AwsServiceException e) {
            int status = e.statusCode();
            String code = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "";
            if (status == 404 || "NoSuchBucketPolicy".equalsIgnoreCase(code)) {
                return new S3BucketPolicySummary("NOT_CONFIGURED", false, null, null);
            }
            if (status == 403 || "AccessDenied".equalsIgnoreCase(code)) {
                return new S3BucketPolicySummary("ACCESS_DENIED", null, null, "Permission denied");
            }
            return new S3BucketPolicySummary("UNAVAILABLE", null, null, code);
        }
    }

    private S3CorsConfiguration inspectCors(String bucketName) {
        try {
            GetBucketCorsResponse response = s3Client.getBucketCors(
                    GetBucketCorsRequest.builder().bucket(bucketName).build()
            );
            List<S3CorsRule> rules = new ArrayList<>();
            for (CORSRule r : response.corsRules()) {
                rules.add(new S3CorsRule(r.id(), r.allowedOrigins(), r.allowedMethods(), r.allowedHeaders(), r.exposeHeaders(), r.maxAgeSeconds()));
            }
            return new S3CorsConfiguration("CONFIGURED", rules);
        } catch (AwsServiceException e) {
            int status = e.statusCode();
            String code = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "";
            if (status == 404 || "NoSuchCORSConfiguration".equalsIgnoreCase(code)) {
                return new S3CorsConfiguration("NOT_CONFIGURED", Collections.emptyList());
            }
            if (status == 403 || "AccessDenied".equalsIgnoreCase(code)) {
                return new S3CorsConfiguration("ACCESS_DENIED", Collections.emptyList());
            }
            return new S3CorsConfiguration("UNAVAILABLE", Collections.emptyList());
        }
    }

    private S3LifecycleConfiguration inspectLifecycle(String bucketName) {
        try {
            GetBucketLifecycleConfigurationResponse response = s3Client.getBucketLifecycleConfiguration(
                    GetBucketLifecycleConfigurationRequest.builder().bucket(bucketName).build()
            );
            List<S3LifecycleRule> rules = new ArrayList<>();
            for (LifecycleRule r : response.rules()) {
                Integer expDays = r.expiration() != null ? r.expiration().days() : null;
                Integer noncurrentDays = r.noncurrentVersionExpiration() != null ? r.noncurrentVersionExpiration().noncurrentDays() : null;
                Integer abortDays = r.abortIncompleteMultipartUpload() != null ? r.abortIncompleteMultipartUpload().daysAfterInitiation() : null;
                rules.add(new S3LifecycleRule(r.id(), r.statusAsString(), r.prefix(), expDays, noncurrentDays, abortDays));
            }
            return new S3LifecycleConfiguration("CONFIGURED", rules);
        } catch (AwsServiceException e) {
            int status = e.statusCode();
            String code = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "";
            if (status == 404 || "NoSuchLifecycleConfiguration".equalsIgnoreCase(code)) {
                return new S3LifecycleConfiguration("NOT_CONFIGURED", Collections.emptyList());
            }
            if (status == 403 || "AccessDenied".equalsIgnoreCase(code)) {
                return new S3LifecycleConfiguration("ACCESS_DENIED", Collections.emptyList());
            }
            return new S3LifecycleConfiguration("UNAVAILABLE", Collections.emptyList());
        }
    }

    private Map<String, String> inspectTags(String bucketName) {
        try {
            GetBucketTaggingResponse response = s3Client.getBucketTagging(
                    GetBucketTaggingRequest.builder().bucket(bucketName).build()
            );
            if (!response.hasTagSet()) return Collections.emptyMap();
            return response.tagSet().stream().collect(Collectors.toMap(Tag::key, Tag::value, (k1, k2) -> k1));
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}