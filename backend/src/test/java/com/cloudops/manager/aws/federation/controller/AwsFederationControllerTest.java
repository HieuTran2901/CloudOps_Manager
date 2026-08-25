package com.cloudops.manager.aws.federation.controller;

import com.cloudops.manager.aws.federation.model.AwsAccountContext;
import com.cloudops.manager.aws.federation.model.FederationRequest;
import com.cloudops.manager.aws.federation.model.FederationResult;
import com.cloudops.manager.aws.federation.model.FederationStatus;
import com.cloudops.manager.aws.federation.service.AwsFederationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AwsFederationController.class)
class AwsFederationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AwsFederationService federationService;

    @Test
    @DisplayName("POST /api/v1/aws/federation/assume-role returns ApiResponse<FederationResult>")
    void testAssumeRoleEndpoint() throws Exception {
        FederationResult mockResult = new FederationResult(
                FederationStatus.FEDERATED,
                "123456789012",
                "arn:aws:iam::123456789012:role/TestRole",
                "test-session",
                "us-east-1",
                "Account federation successful.",
                Instant.now()
        );
        when(federationService.federateAccount(any())).thenReturn(mockResult);

        FederationRequest request = new FederationRequest(
                "123456789012",
                "arn:aws:iam::123456789012:role/TestRole",
                "test-session",
                "us-east-1",
                null
        );

        mockMvc.perform(post("/api/v1/aws/federation/assume-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("FEDERATED"))
                .andExpect(jsonPath("$.data.targetAccountId").value("123456789012"));
    }

    @Test
    @DisplayName("GET /api/v1/aws/federation/current-context returns current active context")
    void testCurrentContextEndpoint() throws Exception {
        AwsAccountContext mockContext = new AwsAccountContext(
                "123456789012",
                "Primary Account",
                "us-east-1",
                null,
                true,
                FederationStatus.FEDERATED
        );
        when(federationService.getCurrentContext()).thenReturn(mockContext);

        mockMvc.perform(get("/api/v1/aws/federation/current-context"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("123456789012"))
                .andExpect(jsonPath("$.data.isCurrent").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/aws/federation/accounts returns configured account list")
    void testListAccountsEndpoint() throws Exception {
        AwsAccountContext mockContext = new AwsAccountContext(
                "123456789012",
                "Primary Account",
                "us-east-1",
                null,
                true,
                FederationStatus.FEDERATED
        );
        when(federationService.listConfiguredAccounts()).thenReturn(List.of(mockContext));

        mockMvc.perform(get("/api/v1/aws/federation/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].accountId").value("123456789012"));
    }
}