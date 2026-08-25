package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.CloudResourceType;
import com.cloudops.manager.aws.discovery.model.S3BucketResource;
import com.cloudops.manager.aws.discovery.model.S3DetailResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.GetBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.GetBucketCorsResponse;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionRequest;
import software.amazon.awssdk.services.s3.model.GetBucketEncryptionResponse;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetBucketLifecycleConfigurationResponse;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.GetBucketPolicyResponse;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingRequest;
import software.amazon.awssdk.services.s3.model.GetBucketTaggingResponse;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningRequest;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockRequest;
import software.amazon.awssdk.services.s3.model.GetPublicAccessBlockResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.LifecycleExpiration;
import software.amazon.awssdk.services.s3.model.LifecycleRule;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PublicAccessBlockConfiguration;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionByDefault;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionConfiguration;
import software.amazon.awssdk.services.s3.model.ServerSideEncryptionRule;
import software.amazon.awssdk.services.s3.model.Tag;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsS3ProviderTest {

    @Mock
    private S3Client s3Client;

    private AwsS3Provider s3Provider;

    @BeforeEach
    void setUp() {
        s3Provider = new AwsS3Provider(s3Client);
    }

    @Test
    @DisplayName("Should successfully list S3 buckets")
    void shouldListS3Buckets() {
        Bucket b1 = Bucket.builder().name("alpha-bucket").creationDate(Instant.now()).build();
        Bucket b2 = Bucket.builder().name("beta-bucket").creationDate(Instant.now()).build();

        when(s3Client.listBuckets()).thenReturn(ListBucketsResponse.builder().buckets(b1, b2).build());

        List<S3BucketResource> buckets = s3Provider.listBuckets("us-east-1", "123456789012");

        assertThat(buckets).hasSize(2);
        assertThat(buckets.get(0).resourceId()).isEqualTo("alpha-bucket");
        assertThat(buckets.get(0).resourceType()).isEqualTo(CloudResourceType.S3_BUCKET);
        assertThat(buckets.get(0).arn()).isEqualTo("arn:aws:s3:::alpha-bucket");
    }

    @Test
    @DisplayName("Should successfully inspect S3 bucket details with full configurations")
    void shouldInspectBucketWithFullConfiguration() {
        Instant created = Instant.now();
        Bucket b = Bucket.builder().name("prod-data").creationDate(created).build();
        when(s3Client.listBuckets()).thenReturn(ListBucketsResponse.builder().buckets(b).build());
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

        // Public Access Block
        PublicAccessBlockConfiguration pab = PublicAccessBlockConfiguration.builder()
                .blockPublicAcls(true).ignorePublicAcls(true).blockPublicPolicy(true).restrictPublicBuckets(true)
                .build();
        when(s3Client.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class)))
                .thenReturn(GetPublicAccessBlockResponse.builder().publicAccessBlockConfiguration(pab).build());

        // Encryption
        ServerSideEncryptionRule rule = ServerSideEncryptionRule.builder()
                .applyServerSideEncryptionByDefault(ServerSideEncryptionByDefault.builder()
                        .sseAlgorithm(ServerSideEncryption.AES256).build())
                .bucketKeyEnabled(true)
                .build();
        when(s3Client.getBucketEncryption(any(GetBucketEncryptionRequest.class)))
                .thenReturn(GetBucketEncryptionResponse.builder()
                        .serverSideEncryptionConfiguration(ServerSideEncryptionConfiguration.builder().rules(rule).build()).build());

        // Versioning
        when(s3Client.getBucketVersioning(any(GetBucketVersioningRequest.class)))
                .thenReturn(GetBucketVersioningResponse.builder().status("Enabled").mfaDelete("Disabled").build());

        // Policy
        when(s3Client.getBucketPolicy(any(GetBucketPolicyRequest.class)))
                .thenReturn(GetBucketPolicyResponse.builder().policy("{\"Version\":\"2012-10-17\"}").build());

        // CORS
        CORSRule corsRule = CORSRule.builder().id("cors-1").allowedOrigins("https://example.com").allowedMethods("GET").build();
        when(s3Client.getBucketCors(any(GetBucketCorsRequest.class)))
                .thenReturn(GetBucketCorsResponse.builder().corsRules(corsRule).build());

        // Lifecycle
        LifecycleRule lcRule = LifecycleRule.builder().id("lc-1").status("Enabled").prefix("logs/")
                .expiration(LifecycleExpiration.builder().days(90).build()).build();
        when(s3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class)))
                .thenReturn(GetBucketLifecycleConfigurationResponse.builder().rules(lcRule).build());

        // Tags
        when(s3Client.getBucketTagging(any(GetBucketTaggingRequest.class)))
                .thenReturn(GetBucketTaggingResponse.builder().tagSet(List.of(Tag.builder().key("Env").value("Prod").build())).build());

        Optional<S3DetailResource> result = s3Provider.getBucket("prod-data", "us-east-1", "123456789012");

        assertThat(result).isPresent();
        S3DetailResource d = result.get();
        assertThat(d.bucketName()).isEqualTo("prod-data");
        assertThat(d.arn()).isEqualTo("arn:aws:s3:::prod-data");
        assertThat(d.publicAccessBlock().status()).isEqualTo("CONFIGURED");
        assertThat(d.publicAccessBlock().blockPublicPolicy()).isTrue();
        assertThat(d.encryption().status()).isEqualTo("CONFIGURED");
        assertThat(d.encryption().algorithm()).isEqualTo("AES256");
        assertThat(d.versioning().versioningStatus()).isEqualTo("Enabled");
        assertThat(d.policy().status()).isEqualTo("CONFIGURED");
        assertThat(d.policy().hasPolicy()).isTrue();
        assertThat(d.cors().status()).isEqualTo("CONFIGURED");
        assertThat(d.cors().rules()).hasSize(1);
        assertThat(d.lifecycle().status()).isEqualTo("CONFIGURED");
        assertThat(d.lifecycle().rules().get(0).expirationDays()).isEqualTo(90);
        assertThat(d.tags()).containsEntry("Env", "Prod");
    }

    @Test
    @DisplayName("Should handle missing sub-configurations as NOT_CONFIGURED")
    void shouldHandleMissingSubConfigurations() {
        when(s3Client.listBuckets()).thenReturn(ListBucketsResponse.builder().build());
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

        S3Exception notFoundEx = (S3Exception) S3Exception.builder().statusCode(404).message("Not Found").build();

        when(s3Client.getPublicAccessBlock(any(GetPublicAccessBlockRequest.class))).thenThrow(notFoundEx);
        when(s3Client.getBucketEncryption(any(GetBucketEncryptionRequest.class))).thenThrow(notFoundEx);
        when(s3Client.getBucketPolicy(any(GetBucketPolicyRequest.class))).thenThrow(notFoundEx);
        when(s3Client.getBucketCors(any(GetBucketCorsRequest.class))).thenThrow(notFoundEx);
        when(s3Client.getBucketLifecycleConfiguration(any(GetBucketLifecycleConfigurationRequest.class))).thenThrow(notFoundEx);
        when(s3Client.getBucketTagging(any(GetBucketTaggingRequest.class))).thenThrow(notFoundEx);

        when(s3Client.getBucketVersioning(any(GetBucketVersioningRequest.class)))
                .thenReturn(GetBucketVersioningResponse.builder().build());

        Optional<S3DetailResource> result = s3Provider.getBucket("empty-bucket", "us-east-1", "123456789012");

        assertThat(result).isPresent();
        S3DetailResource d = result.get();
        assertThat(d.publicAccessBlock().status()).isEqualTo("NOT_CONFIGURED");
        assertThat(d.encryption().status()).isEqualTo("NOT_CONFIGURED");
        assertThat(d.policy().status()).isEqualTo("NOT_CONFIGURED");
        assertThat(d.cors().status()).isEqualTo("NOT_CONFIGURED");
        assertThat(d.lifecycle().status()).isEqualTo("NOT_CONFIGURED");
    }

    @Test
    @DisplayName("Should return Optional.empty() when bucket does not exist")
    void shouldReturnEmptyWhenBucketNotFound() {
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().message("The specified bucket does not exist").build());

        Optional<S3DetailResource> result = s3Provider.getBucket("missing-bucket", "us-east-1", "123456789012");

        assertThat(result).isEmpty();
    }
}