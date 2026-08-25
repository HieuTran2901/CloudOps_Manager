package com.cloudops.manager.operations.evidence.service;

import com.cloudops.manager.operations.evidence.model.EvidenceFreshnessState;
import com.cloudops.manager.operations.evidence.model.EvidenceLifecycleRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EvidenceLifecycleServiceTest {

    private EvidenceLifecycleService evidenceService;

    @BeforeEach
    void setUp() {
        evidenceService = new EvidenceLifecycleService("ap-southeast-2");
    }

    @Test
    @DisplayName("Computes deterministic evidence freshness states based on age")
    void testComputeFreshness() {
        assertEquals(EvidenceFreshnessState.FRESH, evidenceService.computeFreshness(100));
        assertEquals(EvidenceFreshnessState.AGING, evidenceService.computeFreshness(600));
        assertEquals(EvidenceFreshnessState.STALE, evidenceService.computeFreshness(1800));
        assertEquals(EvidenceFreshnessState.EXPIRED, evidenceService.computeFreshness(5000));
    }

    @Test
    @DisplayName("Retrieves evidence lifecycles across core analytical domains")
    void testGetEvidenceLifecycles() {
        List<EvidenceLifecycleRecord> records = evidenceService.getEvidenceLifecycles("351405419700", "ap-southeast-2");
        assertNotNull(records);
        assertEquals(5, records.size());
        for (EvidenceLifecycleRecord r : records) {
            assertEquals("351405419700", r.accountId());
            assertEquals("ap-southeast-2", r.region());
            assertNotNull(r.evidenceDigest());
        }
    }
}