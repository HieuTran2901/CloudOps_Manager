package com.cloudops.manager.aws.security.engine;

import com.cloudops.manager.aws.discovery.model.Ec2DetailResource;
import com.cloudops.manager.aws.discovery.model.Ec2NetworkInterfaceDetail;
import com.cloudops.manager.aws.discovery.model.SecurityGroupDetailResource;
import com.cloudops.manager.aws.discovery.model.SecurityGroupRuleDetail;
import com.cloudops.manager.aws.security.model.ExposureStatus;
import com.cloudops.manager.aws.security.model.SecurityExposureResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExposureAnalysisEngineTest {

    private final ExposureAnalysisEngine engine = new ExposureAnalysisEngine();

    @Test
    @DisplayName("Should detect EXPOSED status for public EC2 with open SSH security group")
    void shouldDetectExposedEc2() {
        Ec2NetworkInterfaceDetail eni = new Ec2NetworkInterfaceDetail(
                "eni-1", "sub-1", "vpc-1", "10.0.0.1", List.of("10.0.0.1"), "54.200.1.1",
                "00:11", List.of("sg-open"), List.of("open-sg"), "in-use", "", "interface"
        );

        Ec2DetailResource ec2 = new Ec2DetailResource(
                "i-1", "web", "arn", "123", "us-east-1", "t3.micro", "x86", "Linux", "Linux",
                "ami-1", null, Instant.now(), "running", null, null, "disabled", "us-east-1a", null,
                "default", "vpc-1", "sub-1", "10.0.0.1", "54.200.1.1", "dns", "dns", List.of(), List.of(eni), Map.of(), Instant.now()
        );

        SecurityGroupRuleDetail openRule = new SecurityGroupRuleDetail(
                "tcp", 22, 22, List.of("0.0.0.0/0"), List.of(), List.of(), List.of(), "SSH"
        );
        SecurityGroupDetailResource sg = new SecurityGroupDetailResource(
                "sg-open", "arn", "open-sg", "desc", "vpc-1", "123", "123", "us-east-1", List.of(openRule), List.of(), Map.of(), Instant.now()
        );

        List<SecurityExposureResult> exposures = engine.evaluateExposures(List.of(ec2), List.of(sg), "123", "us-east-1");
        assertThat(exposures).hasSize(1);
        assertThat(exposures.get(0).status()).isEqualTo(ExposureStatus.EXPOSED);
    }
}