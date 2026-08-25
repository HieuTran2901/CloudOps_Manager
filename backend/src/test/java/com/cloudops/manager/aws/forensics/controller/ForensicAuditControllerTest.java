package com.cloudops.manager.aws.forensics.controller;

import com.cloudops.manager.aws.forensics.model.ForensicExportFormat;
import com.cloudops.manager.aws.forensics.model.ForensicExportResult;
import com.cloudops.manager.aws.forensics.model.ForensicMetadata;
import com.cloudops.manager.aws.forensics.service.ForensicAuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ForensicAuditController.class)
class ForensicAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ForensicAuditService auditService;

    @Test
    @DisplayName("GET /api/v1/aws/forensics/export should return forensic attachment with digest header")
    void shouldReturnExport() throws Exception {
        ForensicMetadata meta = new ForensicMetadata("bundle-1", "123", "us-east-1", Instant.now(), ForensicExportFormat.JSON, "digest-abc", 0, Map.of());
        ForensicExportResult result = new ForensicExportResult(meta, "{}", "digest-abc", "application/json", "forensic.json");
        when(auditService.exportForensics(any(), any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/aws/forensics/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Forensic-SHA256-Digest", "digest-abc"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"forensic.json\""));
    }
}