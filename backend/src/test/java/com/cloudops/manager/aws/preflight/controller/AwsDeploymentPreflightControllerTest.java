package com.cloudops.manager.aws.preflight.controller;

import com.cloudops.manager.aws.preflight.model.AwsCapabilityCheck;
import com.cloudops.manager.aws.preflight.model.DeploymentPreflightResult;
import com.cloudops.manager.aws.preflight.model.PreflightStatus;
import com.cloudops.manager.aws.preflight.service.AwsDeploymentPreflightService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AwsDeploymentPreflightController.class)
class AwsDeploymentPreflightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AwsDeploymentPreflightService preflightService;

    @Test
    @DisplayName("GET /api/v1/aws/preflight returns DeploymentPreflightResult contract")
    void testPreflightEndpoint() throws Exception {
        DeploymentPreflightResult mockResult = new DeploymentPreflightResult(
                PreflightStatus.PASS,
                "123456789012",
                "us-east-1",
                "arn:aws:iam::123456789012:user/admin",
                List.of(new AwsCapabilityCheck("STS", "sts:GetCallerIdentity", PreflightStatus.PASS, "OK")),
                Instant.now(),
                "All capabilities pass."
        );
        when(preflightService.runPreflightCheck(any())).thenReturn(mockResult);

        mockMvc.perform(get("/api/v1/aws/preflight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallStatus").value("PASS"))
                .andExpect(jsonPath("$.data.accountId").value("123456789012"))
                .andExpect(jsonPath("$.data.capabilityChecks").isArray());
    }
}