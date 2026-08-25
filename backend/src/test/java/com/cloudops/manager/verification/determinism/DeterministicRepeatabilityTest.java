package com.cloudops.manager.verification.determinism;

import com.cloudops.manager.aws.forensics.export.ForensicCsvExporter;
import com.cloudops.manager.aws.forensics.export.ForensicIntegritySigner;
import com.cloudops.manager.aws.forensics.model.ForensicEvidenceItem;
import com.cloudops.manager.aws.forensics.model.ForensicExportResult;
import com.cloudops.manager.aws.security.engine.BlastRadiusAnalysisEngine;
import com.cloudops.manager.aws.security.engine.ReachabilityAnalysisEngine;
import com.cloudops.manager.aws.security.model.BlastRadiusResult;
import com.cloudops.manager.aws.security.model.SecurityReachabilityResult;
import com.cloudops.manager.aws.topology.model.TopologyGraph;
import com.cloudops.manager.aws.topology.model.TopologyNode;
import com.cloudops.manager.verification.fixture.SyntheticEvidenceFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("22D.2 — Deterministic Repeatability Verification")
class DeterministicRepeatabilityTest {

    @Test
    @DisplayName("Verify 10 repeated iterations produce 100% identical outputs and bitwise identical SHA-256 digests")
    void testRepeatableDeterminism() {
        String accountId = SyntheticEvidenceFixtures.SYNTHETIC_ACCOUNT_A;
        String region = SyntheticEvidenceFixtures.REGION_US_EAST_1;

        BlastRadiusAnalysisEngine blastEngine = new BlastRadiusAnalysisEngine();
        ReachabilityAnalysisEngine reachEngine = new ReachabilityAnalysisEngine();
        ForensicIntegritySigner signer = new ForensicIntegritySigner();
        ForensicCsvExporter csvExporter = new ForensicCsvExporter(signer);

        String baselineCsv = null;
        String baselineSha256 = null;
        List<String> baselineReachPath = null;
        int baselineBlastNodeCount = -1;

        for (int iter = 1; iter <= 10; iter++) {
            TopologyGraph graph = SyntheticEvidenceFixtures.createSyntheticGraph(accountId, region);
            TopologyNode root = graph.nodes().get(2);
            TopologyNode target = graph.nodes().get(0);

            BlastRadiusResult blastResult = blastEngine.calculateBlastRadius(root.nodeId(), 5, graph);
            SecurityReachabilityResult reachResult = reachEngine.analyzeReachability(root.nodeId(), target.nodeId(), 5, graph);

            List<ForensicEvidenceItem> evidenceList = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                evidenceList.add(SyntheticEvidenceFixtures.createSyntheticEvidenceItem(accountId, region, "DISCOVERY", String.format("%03d", i)));
            }

            ForensicExportResult csvResult = csvExporter.export(evidenceList, accountId, region);

            String currentCsv = csvResult.content();
            String currentSha256 = csvResult.sha256Digest();

            if (iter == 1) {
                baselineCsv = currentCsv;
                baselineSha256 = currentSha256;
                baselineReachPath = reachResult.path().nodeIds();
                baselineBlastNodeCount = blastResult.traversedNodeCount();
            } else {
                assertEquals(baselineCsv, currentCsv, "CSV serialization must remain 100% deterministic on iteration " + iter);
                assertEquals(baselineSha256, currentSha256, "SHA-256 digest must remain 100% bitwise identical on iteration " + iter);
                assertEquals(baselineReachPath, reachResult.path().nodeIds(), "Reachability path must remain identical on iteration " + iter);
                assertEquals(baselineBlastNodeCount, blastResult.traversedNodeCount(), "Blast radius count must remain identical on iteration " + iter);
            }
        }
    }
}