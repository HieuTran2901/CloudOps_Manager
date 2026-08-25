package com.cloudops.manager.aws.security.controller;

import com.cloudops.manager.aws.security.model.BlastRadiusResult;
import com.cloudops.manager.aws.security.model.ExposureStatus;
import com.cloudops.manager.aws.security.model.SecurityExposureResult;
import com.cloudops.manager.aws.security.service.SecurityAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SecurityAnalysisController.class)
class SecurityAnalysisControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityAnalysisService securityService;

    @Test
    @DisplayName("GET /api/v1/aws/security/blast-radius/{nodeId} should return blast radius")
    void shouldReturnBlastRadius() throws Exception {
        BlastRadiusResult result = new BlastRadiusResult("123:us-east-1:EC2_INSTANCE:i-1", 3, List.of(), List.of(), 0, 0, "123", "us-east-1");
        when(securityService.getBlastRadius(any(), anyInt(), any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/aws/security/blast-radius/123:us-east-1:EC2_INSTANCE:i-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sourceNodeId").value("123:us-east-1:EC2_INSTANCE:i-1"));
    }

    @Test
    @DisplayName("GET /api/v1/aws/security/exposures should return exposure findings")
    void shouldReturnExposures() throws Exception {
        SecurityExposureResult exposure = new SecurityExposureResult("123:us-east-1:EC2_INSTANCE:i-1", "AWS::EC2::Instance", "i-1", ExposureStatus.EXPOSED, Map.of(), "123", "us-east-1");
        when(securityService.getExposures(any())).thenReturn(List.of(exposure));

        mockMvc.perform(get("/api/v1/aws/security/exposures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].status").value("EXPOSED"));
    }
}