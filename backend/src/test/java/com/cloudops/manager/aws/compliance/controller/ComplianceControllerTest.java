package com.cloudops.manager.aws.compliance.controller;

import com.cloudops.manager.aws.compliance.model.ComplianceEvaluationReport;
import com.cloudops.manager.aws.compliance.service.ComplianceEvaluationService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComplianceController.class)
@Import(GlobalExceptionHandler.class)
class ComplianceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComplianceEvaluationService complianceService;

    @Test
    @DisplayName("GET /api/v1/aws/compliance/evaluate should return ComplianceEvaluationReport")
    void shouldReturnComplianceReport() throws Exception {
        ComplianceEvaluationReport report = new ComplianceEvaluationReport(
                "123456789012", "us-east-1", Instant.now(), 2, 2, 0, 0, 0, List.of()
        );

        when(complianceService.evaluateLocal(any(), any())).thenReturn(report);

        mockMvc.perform(get("/api/v1/aws/compliance/evaluate").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("123456789012"))
                .andExpect(jsonPath("$.data.passCount").value(2));
    }
}