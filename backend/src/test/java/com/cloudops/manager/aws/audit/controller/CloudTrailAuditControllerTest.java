package com.cloudops.manager.aws.audit.controller;

import com.cloudops.manager.aws.audit.model.CloudTrailEventResult;
import com.cloudops.manager.aws.audit.service.CloudTrailAuditService;
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

@WebMvcTest(CloudTrailAuditController.class)
@Import(GlobalExceptionHandler.class)
class CloudTrailAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CloudTrailAuditService auditService;

    @Test
    @DisplayName("GET /api/v1/aws/audit/cloudtrail/events should return CloudTrailEventResult")
    void shouldReturnCloudTrailEvents() throws Exception {
        CloudTrailEventResult result = new CloudTrailEventResult(
                "123456789012", "us-east-1", Instant.now().minusSeconds(3600), Instant.now(), 0, List.of(), Instant.now()
        );

        when(auditService.lookupEvents(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(result);

        mockMvc.perform(get("/api/v1/aws/audit/cloudtrail/events?eventName=RunInstances").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("123456789012"));
    }
}