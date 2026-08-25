package com.cloudops.manager.aws.sts;

import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Tag("integration")
class AwsStsIntegrationTest {

    @Autowired
    private AwsIdentityService awsIdentityService;

    @Test
    @DisplayName("Integration: Should connect to live AWS STS and retrieve actual caller identity")
    void shouldRetrieveRealAwsCallerIdentity() {
        CallerIdentity identity = awsIdentityService.getCurrentIdentity();

        assertThat(identity).isNotNull();
        assertThat(identity.accountId()).matches("^[0-9]{12}$");
        assertThat(identity.arn()).startsWith("arn:aws:");
        assertThat(identity.userId()).isNotBlank();
    }
}