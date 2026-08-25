package com.cloudops.manager.aws.drift.service;

import com.cloudops.manager.aws.discovery.model.Ec2DetailResource;
import com.cloudops.manager.aws.drift.model.DriftAttributeDifference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TerraformResourceNormalizerTest {

    private final TerraformResourceNormalizer normalizer = new TerraformResourceNormalizer();

    @Test
    @DisplayName("Should detect drift when instance_type differs")
    void shouldDetectEc2Drift() {
        Ec2DetailResource observed = new Ec2DetailResource(
                "i-123", "web", "arn", "123456789012", "us-east-1",
                "t3.small", "x86_64", "Linux", "Linux", "ami-1", null, Instant.now(),
                "running", null, null, "disabled", "us-east-1a", null, "default", "vpc-1", "subnet-1",
                "10.0.0.1", null, "dns", "dns", List.of(), List.of(), Map.of(), Instant.now()
        );

        Map<String, Object> desired = Map.of("instance_type", "t3.micro", "subnet_id", "subnet-1");
        List<DriftAttributeDifference> diffs = normalizer.compareEc2(desired, observed);

        assertThat(diffs).hasSize(1);
        assertThat(diffs.get(0).attributeName()).isEqualTo("instance_type");
        assertThat(diffs.get(0).desiredValue()).isEqualTo("t3.micro");
        assertThat(diffs.get(0).observedValue()).isEqualTo("t3.small");
    }
}