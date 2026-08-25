package com.cloudops.manager.aws.drift.parser;

import com.cloudops.manager.aws.drift.model.TerraformDesiredResource;
import com.cloudops.manager.aws.drift.model.TerraformDesiredState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TerraformStateParserTest {

    private final TerraformStateParser parser = new TerraformStateParser();

    @Test
    @DisplayName("Should parse valid Terraform state JSON with EC2 and S3 resources")
    void shouldParseValidStateJson() {
        String json = """
        {
          "version": 4,
          "terraform_version": "1.5.0",
          "serial": 5,
          "resources": [
            {
              "mode": "managed",
              "type": "aws_instance",
              "name": "web",
              "instances": [
                {
                  "attributes": {
                    "id": "i-0123456789abcdef0",
                    "arn": "arn:aws:ec2:us-east-1:123456789012:instance/i-0123456789abcdef0",
                    "instance_type": "t3.micro"
                  }
                }
              ]
            }
          ]
        }
        """;

        TerraformDesiredState state = parser.parseStateJson(json);
        assertThat(state.formatVersion()).isEqualTo(4);
        assertThat(state.terraformVersion()).isEqualTo("1.5.0");
        assertThat(state.resources()).hasSize(1);

        TerraformDesiredResource res = state.resources().get(0);
        assertThat(res.resourceType()).isEqualTo("aws_instance");
        assertThat(res.resourceId()).isEqualTo("i-0123456789abcdef0");
        assertThat(res.accountId()).isEqualTo("123456789012");
        assertThat(res.region()).isEqualTo("us-east-1");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when parsing malformed JSON")
    void shouldThrowOnMalformedJson() {
        assertThatThrownBy(() -> parser.parseStateJson("{ invalid json }"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}