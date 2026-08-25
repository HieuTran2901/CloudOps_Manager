package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.IamPolicyStatement;
import com.cloudops.manager.aws.discovery.model.IamTrustStatement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IamPolicyDocumentParserTest {

    @Test
    @DisplayName("Should parse single statement policy JSON")
    void shouldParseSingleStatementPolicy() {
        String json = "{\"Version\":\"2012-10-17\",\"Statement\":{\"Effect\":\"Allow\",\"Action\":\"s3:GetObject\",\"Resource\":\"arn:aws:s3:::mybucket/*\"}}";
        List<IamPolicyStatement> stmts = IamPolicyDocumentParser.parsePolicyStatements(json);

        assertThat(stmts).hasSize(1);
        assertThat(stmts.get(0).effect()).isEqualTo("Allow");
        assertThat(stmts.get(0).actions()).containsExactly("s3:GetObject");
        assertThat(stmts.get(0).resources()).containsExactly("arn:aws:s3:::mybucket/*");
    }

    @Test
    @DisplayName("Should parse URL encoded array statements with Condition")
    void shouldParseUrlEncodedArrayStatements() {
        String json = "%7B%22Statement%22%3A%5B%7B%22Effect%22%3A%22Deny%22%2C%22Action%22%3A%5B%22ec2%3AStartInstances%22%2C%22ec2%3AStopInstances%22%5D%2C%22Resource%22%3A%22%2A%22%2C%22Condition%22%3A%7B%22Bool%22%3A%7B%22aws%3AMultiFactorAuthPresent%22%3A%22false%22%7D%7D%7D%5D%7D";
        List<IamPolicyStatement> stmts = IamPolicyDocumentParser.parsePolicyStatements(json);

        assertThat(stmts).hasSize(1);
        assertThat(stmts.get(0).effect()).isEqualTo("Deny");
        assertThat(stmts.get(0).actions()).contains("ec2:StartInstances", "ec2:StopInstances");
        assertThat(stmts.get(0).condition()).isNotNull();
    }

    @Test
    @DisplayName("Should parse trust policy with principal mapping")
    void shouldParseTrustPolicy() {
        String json = "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"arn:aws:iam::123:root\"],\"Service\":\"lambda.amazonaws.com\"},\"Action\":\"sts:AssumeRole\"}]}";
        List<IamTrustStatement> stmts = IamPolicyDocumentParser.parseTrustStatements(json);

        assertThat(stmts).hasSize(1);
        assertThat(stmts.get(0).principals()).contains("AWS:arn:aws:iam::123:root", "Service:lambda.amazonaws.com");
    }
}