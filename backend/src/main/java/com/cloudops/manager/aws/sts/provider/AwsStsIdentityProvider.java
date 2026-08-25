package com.cloudops.manager.aws.sts.provider;

import com.cloudops.manager.aws.sts.model.AssumeRoleRequest;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.common.exception.AwsErrorTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleResponse;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

@Component
public class AwsStsIdentityProvider implements StsIdentityProvider {

    private static final Logger log = LoggerFactory.getLogger(AwsStsIdentityProvider.class);
    private final StsClient stsClient;

    public AwsStsIdentityProvider(StsClient stsClient) {
        this.stsClient = stsClient;
    }

    @Override
    public CallerIdentity getCallerIdentity() {
        try {
            GetCallerIdentityResponse response = stsClient.getCallerIdentity(
                GetCallerIdentityRequest.builder().build()
            );

            log.info("Successfully resolved AWS caller identity for account: {}", response.account());
            return new CallerIdentity(response.account(), response.arn(), response.userId());
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("STS:GetCallerIdentity", e, log);
        }
    }

    @Override
    public AssumedRoleSession assumeRole(AssumeRoleRequest request) {
        try {
            software.amazon.awssdk.services.sts.model.AssumeRoleRequest.Builder builder =
                software.amazon.awssdk.services.sts.model.AssumeRoleRequest.builder()
                    .roleArn(request.roleArn())
                    .roleSessionName(request.sessionName());

            if (request.externalId() != null && !request.externalId().isBlank()) {
                builder.externalId(request.externalId());
            }
            if (request.durationSeconds() != null) {
                builder.durationSeconds(request.durationSeconds());
            }

            AssumeRoleResponse response = stsClient.assumeRole(builder.build());
            Credentials credentials = response.credentials();

            log.info("Successfully assumed role: {} for session: {}", request.roleArn(), request.sessionName());

            return new AssumedRoleSession(
                credentials.accessKeyId(),
                credentials.secretAccessKey(),
                credentials.sessionToken(),
                credentials.expiration(),
                response.assumedRoleUser() != null ? response.assumedRoleUser().arn() : request.roleArn()
            );
        } catch (Exception e) {
            throw AwsErrorTranslator.translate("STS:AssumeRole", e, log);
        }
    }
}