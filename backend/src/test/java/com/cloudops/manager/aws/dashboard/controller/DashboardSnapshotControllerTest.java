package com.cloudops.manager.aws.dashboard.controller;

import com.cloudops.manager.aws.dashboard.model.DashboardSnapshot;
import com.cloudops.manager.aws.dashboard.model.DashboardSnapshotStatus;
import com.cloudops.manager.aws.dashboard.service.DashboardSnapshotService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardSnapshotController.class)
@ActiveProfiles("dev")
class DashboardSnapshotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardSnapshotService snapshotService;

    @Test
    void shouldReturnDashboardSnapshot() throws Exception {
        DashboardSnapshot mockSnapshot = new DashboardSnapshot(
                "351405419700",
                "ap-southeast-2",
                DashboardSnapshotStatus.LIVE,
                Instant.now(),
                Instant.now(),
                null, null, null, null
        );

        when(snapshotService.getSnapshot("ap-southeast-2")).thenReturn(mockSnapshot);

        mockMvc.perform(get("/api/v1/aws/dashboard/snapshot?region=ap-southeast-2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("351405419700"))
                .andExpect(jsonPath("$.data.region").value("ap-southeast-2"))
                .andExpect(jsonPath("$.data.snapshotStatus").value("LIVE"));
    }

    @Test
    void shouldRefreshDashboardSnapshot() throws Exception {
        DashboardSnapshot mockSnapshot = new DashboardSnapshot(
                "351405419700",
                "ap-southeast-2",
                DashboardSnapshotStatus.LIVE,
                Instant.now(),
                Instant.now(),
                null, null, null, null
        );

        when(snapshotService.refreshSnapshot("ap-southeast-2")).thenReturn(mockSnapshot);

        mockMvc.perform(post("/api/v1/aws/dashboard/snapshot/refresh?region=ap-southeast-2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.region").value("ap-southeast-2"));
    }
}
