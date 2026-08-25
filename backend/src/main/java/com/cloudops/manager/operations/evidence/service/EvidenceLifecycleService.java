package com.cloudops.manager.operations.evidence.service;

import com.cloudops.manager.operations.evidence.model.EvidenceFreshnessState;
import com.cloudops.manager.operations.evidence.model.EvidenceLifecycleRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class EvidenceLifecycleService {

    private final String defaultRegion;

    public EvidenceLifecycleService(@Value("${cloudops.aws.region:us-east-1}") String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    public List<EvidenceLifecycleRecord> getEvidenceLifecycles(String accountId, String optionalRegion) {
        String region = (optionalRegion != null && !optionalRegion.isBlank()) ? optionalRegion : defaultRegion;
        String acc = (accountId != null && !accountId.isBlank()) ? accountId : "351405419700";

        Instant now = Instant.now();
        List<EvidenceLifecycleRecord> records = new ArrayList<>();

        String[] types = {"DISCOVERY_INVENTORY", "TOPOLOGY_GRAPH", "COMPLIANCE_FINDINGS", "TELEMETRY_METRICS", "FORENSIC_SNAPSHOT"};
        long[] simulatedAges = {45, 120, 180, 30, 90};

        for (int i = 0; i < types.length; i++) {
            long age = simulatedAges[i];
            Instant capturedAt = now.minus(Duration.ofSeconds(age));
            EvidenceFreshnessState state = computeFreshness(age);
            String digest = computeEvidenceDigest(types[i], acc, region, age);

            records.add(new EvidenceLifecycleRecord(
                    types[i],
                    acc,
                    region,
                    capturedAt,
                    capturedAt,
                    now,
                    age,
                    state,
                    digest
            ));
        }

        return records;
    }

    public EvidenceFreshnessState computeFreshness(long ageSeconds) {
        if (ageSeconds <= 300) {
            return EvidenceFreshnessState.FRESH;
        } else if (ageSeconds <= 900) {
            return EvidenceFreshnessState.AGING;
        } else if (ageSeconds <= 3600) {
            return EvidenceFreshnessState.STALE;
        } else {
            return EvidenceFreshnessState.EXPIRED;
        }
    }

    private String computeEvidenceDigest(String type, String accountId, String region, long age) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String raw = type + ":" + accountId + ":" + region + ":" + (age / 60);
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return "UNKNOWN_DIGEST";
        }
    }
}