package com.cloudops.manager.aws.compliance.service;

import com.cloudops.manager.aws.audit.service.CloudTrailAuditService;
import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationReport;
import com.cloudops.manager.aws.compliance.model.ComplianceRule;
import com.cloudops.manager.aws.compliance.rules.ComplianceRuleRegistry;
import com.cloudops.manager.aws.compliance.rules.SecIamMfaRule;
import com.cloudops.manager.aws.compliance.rules.SecSgOpenIngressRule;
import com.cloudops.manager.aws.discovery.model.InventorySummary;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplianceEvaluationServiceTest {

    @Mock
    private AwsResourceDiscoveryService discoveryService;
    @Mock
    private AwsIdentityService identityService;
    @Mock
    private CloudTrailAuditService auditService;

    private ComplianceEvaluationService complianceService;

    @BeforeEach
    void setUp() {
        List<ComplianceRule> rules = List.of(new SecIamMfaRule(), new SecSgOpenIngressRule());
        ComplianceRuleRegistry registry = new ComplianceRuleRegistry(rules);
        complianceService = new ComplianceEvaluationService(registry, discoveryService, identityService, auditService);
        ReflectionTestUtils.setField(complianceService, "defaultRegion", "us-east-1");
    }

    @Test
    @DisplayName("Should evaluate registered rules and produce summary tallies")
    void shouldEvaluateRulesLocally() {
        when(identityService.getCurrentIdentity())
                .thenReturn(new CallerIdentity("123456789012", "arn:aws:iam::123456789012:user/admin", "AIDADMIN"));

        InventorySummary summary = new InventorySummary("123456789012", "us-east-1", 0, Map.of(), List.of(), Instant.now());
        when(discoveryService.discoverAll(any())).thenReturn(summary);
        when(discoveryService.getIamUsers()).thenReturn(List.of());

        ComplianceEvaluationReport report = complianceService.evaluateLocal("us-east-1", null);

        assertThat(report.accountId()).isEqualTo("123456789012");
        assertThat(report.totalRulesEvaluated()).isEqualTo(2);
        assertThat(report.results()).hasSize(2);
    }
}