package com.cloudops.manager.aws.drift.service;

import com.cloudops.manager.aws.discovery.model.Ec2DetailResource;
import com.cloudops.manager.aws.discovery.model.InventorySummary;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.drift.model.*;
import com.cloudops.manager.aws.drift.parser.TerraformStateParser;
import com.cloudops.manager.aws.sts.model.CallerIdentity;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriftComparisonServiceTest {

    @Mock
    private AwsResourceDiscoveryService discoveryService;
    @Mock
    private AwsIdentityService identityService;

    private DriftComparisonService driftService;

    @BeforeEach
    void setUp() {
        TerraformStateParser parser = new TerraformStateParser();
        TerraformResourceNormalizer normalizer = new TerraformResourceNormalizer();
        driftService = new DriftComparisonService(parser, normalizer, discoveryService, identityService);
        ReflectionTestUtils.setField(driftService, "defaultRegion", "us-east-1");
    }

    @Test
    @DisplayName("Should evaluate IN_SYNC when observed matches desired state")
    void shouldEvaluateInSync() {
        when(identityService.getCurrentIdentity())
                .thenReturn(new CallerIdentity("123456789012", "arn", "user"));

        InventorySummary summary = new InventorySummary("123456789012", "us-east-1", 1, Map.of(), List.of(), Instant.now());
        when(discoveryService.discoverAll(any())).thenReturn(summary);

        Ec2DetailResource observed = new Ec2DetailResource(
                "i-123", "web", "arn", "123456789012", "us-east-1",
                "t3.micro", "x86_64", "Linux", "Linux", "ami-1", null, Instant.now(),
                "running", null, null, "disabled", "us-east-1a", null, "default", "vpc-1", "subnet-1",
                "10.0.0.1", null, "dns", "dns", List.of(), List.of(), Map.of(), Instant.now()
        );
        when(discoveryService.getEc2InstanceDetail(eq("i-123"), any())).thenReturn(observed);

        String json = """
        {
          "version": 4,
          "resources": [
            {
              "type": "aws_instance",
              "name": "web",
              "instances": [
                {
                  "attributes": {
                    "id": "i-123",
                    "instance_type": "t3.micro"
                  }
                }
              ]
            }
          ]
        }
        """;

        DriftReport report = driftService.evaluateDrift(json, "us-east-1");
        assertThat(report.totalResources()).isEqualTo(1);
        assertThat(report.inSyncCount()).isEqualTo(1);
        assertThat(report.driftedCount()).isEqualTo(0);
        assertThat(report.resources().get(0).status()).isEqualTo(DriftStatus.IN_SYNC);
    }
}