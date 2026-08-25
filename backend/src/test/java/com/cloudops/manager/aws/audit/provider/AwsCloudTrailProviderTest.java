package com.cloudops.manager.aws.audit.provider;

import com.cloudops.manager.aws.audit.model.CloudTrailEventLookupRequest;
import com.cloudops.manager.aws.audit.model.CloudTrailEventResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.model.*;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AwsCloudTrailProviderTest {

    @Mock
    private CloudTrailClient cloudTrailClient;

    private AwsCloudTrailProvider provider;

    @BeforeEach
    void setUp() {
        provider = new AwsCloudTrailProvider(cloudTrailClient);
    }

    @Test
    @DisplayName("Should parse LookupEvents response and normalize events")
    void shouldParseLookupEvents() {
        CloudTrailEventLookupRequest request = new CloudTrailEventLookupRequest(
                "123456789012", "us-east-1", "RunInstances", null, null, null,
                Instant.now().minusSeconds(3600), Instant.now(), 50
        );

        Resource res = Resource.builder().resourceType("AWS::EC2::Instance").resourceName("i-1234567890abcdef0").build();
        Event ev1 = Event.builder()
                .eventId("ev-1")
                .eventName("RunInstances")
                .eventSource("ec2.amazonaws.com")
                .eventTime(Instant.now())
                .username("alice")
                .readOnly("false")
                .resources(res)
                .accessKeyId("AKIAIOSFODNN7EXAMPLE")
                .build();

        LookupEventsResponse response = LookupEventsResponse.builder().events(ev1).nextToken(null).build();
        when(cloudTrailClient.lookupEvents(any(LookupEventsRequest.class))).thenReturn(response);

        CloudTrailEventResult result = provider.lookupEvents(request, cloudTrailClient);

        assertThat(result.accountId()).isEqualTo("123456789012");
        assertThat(result.region()).isEqualTo("us-east-1");
        assertThat(result.totalEvents()).isEqualTo(1);
        assertThat(result.events().get(0).eventName()).isEqualTo("RunInstances");
        assertThat(result.events().get(0).resources()).hasSize(1);
        assertThat(result.events().get(0).resources().get(0).resourceName()).isEqualTo("i-1234567890abcdef0");
    }
}