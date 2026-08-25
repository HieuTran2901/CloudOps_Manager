package com.cloudops.manager.aws.sts.provider;

import com.cloudops.manager.aws.sts.exception.AwsAuthenticationException;
import com.cloudops.manager.aws.sts.model.AssumeRoleRequest;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.common.exception.AwsAccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.AssumedRoleUser;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;
import software.amazon.awssdk.services.sts.model.StsException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsStsIdentityProviderTest {

    @Mock
    private StsClient stsClient;

    private AwsStsIdentityProvider identityProvider;

    @BeforeEach
    void setUp() {
        identityProvider = new AwsStsIdentityProvider(stsClient);
    }

    @Test
    @DisplayName("Should successfully return CallerIdentity when STS responds")
    void shouldReturnCallerIdentitySuccessfully() {
        GetCallerIdentityResponse mockResponse = GetCallerIdentityResponse.builder()
                .account("123456789012")
                .arn("arn:aws:iam::123456789012:user/cloudops-admin")
                .userId("AIDAXXXXXXXXXXXXXXXXX")
                .build();

        when(stsClient.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenReturn(mockResponse);

        CallerIdentity result = identityProvider.getCallerIdentity();

        assertThat(result).isNotNull();
        assertThat(result.accountId()).isEqualTo("123456789012");
        assertThat(result.arn()).isEqualTo("arn:aws:iam::123456789012:user/cloudops-admin");
        assertThat(result.userId()).isEqualTo("AIDAXXXXXXXXXXXXXXXXX");
        verify(stsClient).getCallerIdentity(any(GetCallerIdentityRequest.class));
    }

    @Test
    @DisplayName("Should throw AwsAccessDeniedException when STS returns 403 AccessDenied")
    void shouldThrowAwsAccessDeniedExceptionOn403() {
        AwsErrorDetails errorDetails = AwsErrorDetails.builder()
                .errorCode("AccessDenied")
                .errorMessage("User is not authorized to perform sts:GetCallerIdentity")
                .build();

        StsException stsException = (StsException) StsException.builder()
                .statusCode(403)
                .awsErrorDetails(errorDetails)
                .message("Access denied")
                .build();

        when(stsClient.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenThrow(stsException);

        assertThatThrownBy(() -> identityProvider.getCallerIdentity())
                .isInstanceOf(AwsAccessDeniedException.class)
                .hasMessageContaining("AWS Access Denied");
    }

    @Test
    @DisplayName("Should throw AwsAuthenticationException when SdkClientException occurs with credential failure")
    void shouldThrowAwsAuthenticationExceptionOnMissingCredentials() {
        SdkClientException sdkException = SdkClientException.create("Unable to load credentials from any provider");

        when(stsClient.getCallerIdentity(any(GetCallerIdentityRequest.class))).thenThrow(sdkException);

        assertThatThrownBy(() -> identityProvider.getCallerIdentity())
                .isInstanceOf(AwsAuthenticationException.class);
    }

    @Test
    @DisplayName("Should successfully assume role and return in-memory session credentials")
    void shouldAssumeRoleSuccessfully() {
        Instant expiry = Instant.now().plusSeconds(3600);
        Credentials credentials = Credentials.builder()
                .accessKeyId("ASIAEXAMPLEKEYID")
                .secretAccessKey("EXAMPLESECRETKEY")
                .sessionToken("EXAMPLESESSIONTOKEN")
                .expiration(expiry)
                .build();

        AssumedRoleUser assumedRoleUser = AssumedRoleUser.builder()
                .arn("arn:aws:sts::123456789012:assumed-role/CloudOpsRole/test-session")
                .assumedRoleId("AROAEXAMPLE:test-session")
                .build();

        AssumeRoleResponse mockResponse = AssumeRoleResponse.builder()
                .credentials(credentials)
                .assumedRoleUser(assumedRoleUser)
                .build();

        when(stsClient.assumeRole(any(software.amazon.awssdk.services.sts.model.AssumeRoleRequest.class)))
                .thenReturn(mockResponse);

        AssumeRoleRequest request = new AssumeRoleRequest(
                "arn:aws:iam::123456789012:role/CloudOpsRole",
                "test-session",
                "ext-id-123",
                3600
        );

        AssumedRoleSession session = identityProvider.assumeRole(request);

        assertThat(session).isNotNull();
        assertThat(session.accessKeyId()).isEqualTo("ASIAEXAMPLEKEYID");
        assertThat(session.secretAccessKey()).isEqualTo("EXAMPLESECRETKEY");
        assertThat(session.sessionToken()).isEqualTo("EXAMPLESESSIONTOKEN");
        assertThat(session.expiration()).isEqualTo(expiry);
        assertThat(session.assumedRoleUserArn()).isEqualTo("arn:aws:sts::123456789012:assumed-role/CloudOpsRole/test-session");
        assertThat(session.toString()).doesNotContain("EXAMPLESECRETKEY");
    }
}