package com.cloudops.manager.verification.stress;

import com.cloudops.manager.aws.forensics.export.ForensicCsvExporter;
import com.cloudops.manager.aws.forensics.export.ForensicIntegritySigner;
import com.cloudops.manager.aws.forensics.export.ForensicJsonExporter;
import com.cloudops.manager.aws.forensics.model.ForensicEvidenceItem;
import com.cloudops.manager.aws.forensics.model.ForensicExportResult;
import com.cloudops.manager.aws.security.engine.BlastRadiusAnalysisEngine;
import com.cloudops.manager.aws.security.engine.ReachabilityAnalysisEngine;
import com.cloudops.manager.aws.security.model.BlastRadiusResult;
import com.cloudops.manager.aws.security.model.ReachabilityStatus;
import com.cloudops.manager.aws.security.model.SecurityReachabilityResult;
import com.cloudops.manager.aws.topology.model.TopologyEdge;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.model.TopologyNode;
import com.cloudops.manager.aws.topology.model.TopologyNodeType;
import com.cloudops.manager.aws.topology.model.TopologyRelationshipType;
import com.cloudops.manager.verification.fixture.SyntheticEvidenceFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("22B.5 — Stress Testing & Scale Performance")
class SyntheticStressAndScaleTest {

    @Test
    @DisplayName("Stress test: 100, 500, 1,000, and 5,000 synthetic resources scale smoothly")
    void testScaleAndStressPerformance() {
        int[] scaleCounts = {100, 500, 1000, 5000};
        String accountId = SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_A;
        String region = SyntheticEvidenceFixtures.REGION_US_EAST_1;

        BlastRadiusAnalysisEngine blastEngine = new BlastRadiusAnalysisEngine();
        ReachabilityAnalysisEngine reachEngine = new ReachabilityAnalysisEngine();
        ForensicIntegritySigner signer = new ForensicIntegritySigner();
        ForensicJsonExporter jsonExporter = new ForensicJsonExporter(signer);
        ForensicCsvExporter csvExporter = new ForensicCsvExporter(signer);

        for (int count : scaleCounts) {
            long startGraph = System.currentTimeMillis();

            List<TopologyNode> nodes = new ArrayList<>(count);
            List<TopologyEdge> edges = new ArrayList<>(count);
            List<ForensicEvidenceItem> evidenceItems = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                TopologyNode node = SyntheticEvidenceFixtures.createSyntheticNode(
                        accountId, region, TopologyNodeType.EC2_INSTANCE, "i-" + String.format("%06d", i)
                );
                nodes.add(node);
                evidenceItems.add(SyntheticEvidenceFixtures.createSyntheticEvidenceItem(accountId, region, "DISCOVERY", String.valueOf(i)));

                if (i > 0) {
                    TopologyEdge edge = SyntheticEvidenceFixtures.createSyntheticEdge(
                            nodes.get(i - 1), node, TopologyRelationshipType.EC2_IN_SUBNET, accountId, region
                    );
                    edges.add(edge);
                }
            }

            TopologyGraph graph = new TopologyGraph(
                    accountId, region, Instant.now(), nodes.size(), edges.size(), nodes, edges
            );

            long graphDuration = System.currentTimeMillis() - startGraph;

            // Measure Blast Radius computation
            long startBlast = System.currentTimeMillis();
            BlastRadiusResult blast = blastEngine.calculateBlastRadius(nodes.get(0).nodeId(), 10, graph);
            long blastDuration = System.currentTimeMillis() - startBlast;

            // Measure Shortest-Path Reachability
            long startReach = System.currentTimeMillis();
            SecurityReachabilityResult reach = reachEngine.analyzeReachability(nodes.get(0).nodeId(), nodes.get(Math.min(count - 1, 50)).nodeId(), 60, graph);
            long reachDuration = System.currentTimeMillis() - startReach;

            // Measure Forensic JSON Export
            long startJson = System.currentTimeMillis();
            ForensicExportResult jsonResult = jsonExporter.export(evidenceItems, accountId, region);
            long jsonDuration = System.currentTimeMillis() - startJson;

            // Measure Forensic CSV Export
            long startCsv = System.currentTimeMillis();
            ForensicExportResult csvResult = csvExporter.export(evidenceItems, accountId, region);
            long csvDuration = System.currentTimeMillis() - startCsv;

            assertNotNull(blast);
            assertNotNull(reach);
            assertEquals(ReachabilityStatus.REACHABLE, reach.status());
            assertNotNull(jsonResult);
            assertNotNull(csvResult);

            assertTrue(blastDuration < 1500, "Blast radius took " + blastDuration + "ms for " + count + " nodes");
            assertTrue(reachDuration < 1500, "Reachability took " + reachDuration + "ms for " + count + " nodes");
            assertTrue(jsonDuration < 2500, "JSON serialization took " + jsonDuration + "ms for " + count + " items");
            assertTrue(csvDuration < 2500, "CSV serialization took " + csvDuration + "ms for " + count + " items");
        }
    }
}