package com.cloudops.manager.verification.resilience;

import com.cloudops.manager.aws.discovery.model.InventorySummary;
import com.cloudops.manager.aws.security.engine.BlastRadiusAnalysisEngine;
import com.cloudops.manager.aws.security.engine.ReachabilityAnalysisEngine;
import com.cloudops.manager.aws.security.model.BlastRadiusResult;
import com.cloudops.manager.aws.security.model.ReachabilityStatus;
import com.cloudops.manager.aws.security.model.SecurityReachabilityResult;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("22C.2 — Partial Evidence & Empty State Semantics")
class PartialEvidenceResilienceTest {

    @Test
    @DisplayName("Verify empty graph produces explicit empty result without fabricating speculative nodes")
    void testEmptyGraphResilience() {
        TopologyGraph emptyGraph = new TopologyGraph(
                "acc-123", "us-east-1", Instant.now(), 0, 0, Collections.emptyList(), Collections.emptyList()
        );

        BlastRadiusAnalysisEngine blastEngine = new BlastRadiusAnalysisEngine();
        BlastRadiusResult blastResult = blastEngine.calculateBlastRadius("non-existent-node", 3, emptyGraph);
        assertNotNull(blastResult);
        assertEquals(0, blastResult.traversedNodeCount());
        assertEquals(0, blastResult.traversedEdgeCount());

        ReachabilityAnalysisEngine reachEngine = new ReachabilityAnalysisEngine();
        SecurityReachabilityResult reachResult = reachEngine.analyzeReachability("node-1", "node-2", 3, emptyGraph);
        assertNotNull(reachResult);
        assertEquals(ReachabilityStatus.NOT_REACHABLE, reachResult.status());
        assertNull(reachResult.path());
    }

    @Test
    @DisplayName("Verify empty inventory summary maintains consistent contract")
    void testEmptyInventorySummary() {
        InventorySummary emptyInv = new InventorySummary(
                "acc-123", "us-east-1", 0, Map.of(), Collections.emptyList(), Instant.now()
        );

        assertEquals(0, emptyInv.totalCount());
        assertTrue(emptyInv.resources().isEmpty());
    }
}