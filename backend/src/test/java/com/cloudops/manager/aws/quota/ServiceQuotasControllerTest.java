package com.cloudops.manager.aws.quota;

import com.cloudops.manager.aws.quota.controller.ServiceQuotasController;
import com.cloudops.manager.aws.quota.model.QuotaStatus;
import com.cloudops.manager.aws.quota.model.QuotaUtilizationReport;
import com.cloudops.manager.aws.quota.model.ServiceQuotaItem;
import com.cloudops.manager.aws.quota.service.ServiceQuotasService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServiceQuotasController.class)
class ServiceQuotasControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServiceQuotasService serviceQuotasService;

    @Test
    @DisplayName("GET /api/v1/quotas returns standard ApiResponse with quota report including UNKNOWN counts")
    void testGetQuotaReportApiContract() throws Exception {
        ServiceQuotaItem item1 = new ServiceQuotaItem(
                "vpc", "Amazon Virtual Private Cloud", "L-F678F13E", "VPCs per Region",
                5.0, 4.0, 80.00, QuotaStatus.WARNING, "ap-southeast-2", "VPC_DISCOVERY", "Count", true, Instant.now()
        );

        ServiceQuotaItem item2 = new ServiceQuotaItem(
                "iam", "AWS Identity and Access Management", "L-SOME", "Roles per account",
                1000.0, null, null, QuotaStatus.UNKNOWN, "ap-southeast-2", "UNAVAILABLE", "Count", false, Instant.now()
        );

        QuotaUtilizationReport report = new QuotaUtilizationReport(
                "351405419700",
                "ap-southeast-2",
                2,
                0,
                1,
                0,
                1,
                80.00,
                List.of(item1, item2),
                Map.of("WARNING", 1, "UNKNOWN", 1),
                Instant.now()
        );

        when(serviceQuotasService.getQuotaUtilizationReport(anyString())).thenReturn(report);

        mockMvc.perform(get("/api/v1/quotas?region=ap-southeast-2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("351405419700"))
                .andExpect(jsonPath("$.data.region").value("ap-southeast-2"))
                .andExpect(jsonPath("$.data.totalQuotasTracked").value(2))
                .andExpect(jsonPath("$.data.warningCount").value(1))
                .andExpect(jsonPath("$.data.unknownCount").value(1))
                .andExpect(jsonPath("$.data.quotas[0].quotaCode").value("L-F678F13E"))
                .andExpect(jsonPath("$.data.quotas[0].status").value("WARNING"))
                .andExpect(jsonPath("$.data.quotas[1].status").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.statusSummary.UNKNOWN").value(1));
    }
}
