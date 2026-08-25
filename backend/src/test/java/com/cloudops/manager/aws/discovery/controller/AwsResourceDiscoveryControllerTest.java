package com.cloudops.manager.aws.discovery.controller;

import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.observability.service.AwsObservabilityService;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AwsResourceDiscoveryController.class)
@Import(GlobalExceptionHandler.class)
class AwsResourceDiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AwsResourceDiscoveryService discoveryService;

    @MockBean
    private AwsObservabilityService observabilityService;

    @Test
    @DisplayName("GET /api/v1/aws/resources should return InventorySummary")
    void shouldReturnInventorySummary() throws Exception {
        InventorySummary summary = new InventorySummary(
                "123456789012", "us-east-1", 0,
                Map.of(CloudResourceType.EC2_INSTANCE, 0), Collections.emptyList(), Instant.now()
        );

        when(discoveryService.discoverAll(null)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/aws/resources").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("123456789012"));
    }

    @Test
    @DisplayName("GET /api/v1/aws/resources/accounts/{accountId} should return cross-account InventorySummary")
    void shouldReturnCrossAccountInventorySummary() throws Exception {
        InventorySummary summary = new InventorySummary(
                "987654321098", "us-east-1", 0,
                Map.of(CloudResourceType.EC2_INSTANCE, 0), Collections.emptyList(), Instant.now()
        );

        when(discoveryService.discoverAccount(any(AwsAccountTarget.class))).thenReturn(summary);

        mockMvc.perform(get("/api/v1/aws/resources/accounts/987654321098?roleArn=arn:aws:iam::987654321098:role/AuditRole")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("987654321098"));
    }
}