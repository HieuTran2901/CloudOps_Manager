package com.cloudops.manager.aws.topology.controller;

import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.model.TopologyNode;
import com.cloudops.manager.aws.topology.model.TopologyNodeType;
import com.cloudops.manager.aws.topology.service.TopologyQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TopologyController.class)
class TopologyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TopologyQueryService topologyService;

    @Test
    @DisplayName("GET /api/v1/aws/topology should return topology graph")
    void shouldReturnTopology() throws Exception {
        TopologyGraph graph = new TopologyGraph("123", "us-east-1", Instant.now(), 0, 0, List.of(), List.of());
        when(topologyService.getTopology(any())).thenReturn(graph);

        mockMvc.perform(get("/api/v1/aws/topology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value("123"));
    }

    @Test
    @DisplayName("GET /api/v1/aws/topology/nodes/{id} should return node when found")
    void shouldReturnNode() throws Exception {
        TopologyNode node = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, "i-123", "123", "us-east-1", Map.of());
        when(topologyService.findNode(any(), any())).thenReturn(Optional.of(node));

        mockMvc.perform(get("/api/v1/aws/topology/nodes/123:us-east-1:EC2_INSTANCE:i-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resourceId").value("i-123"));
    }
}