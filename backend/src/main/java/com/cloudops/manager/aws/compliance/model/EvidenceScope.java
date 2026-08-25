package com.cloudops.manager.aws.compliance.model;

import java.util.Objects;

public record EvidenceScope(
    String accountId,
    String region
) {
    public boolean matches(EvidenceScope other) {
        if (other == null) return false;
        if (!Objects.equals(this.accountId, other.accountId)) return false;
        if ("global".equalsIgnoreCase(this.region) || "global".equalsIgnoreCase(other.region)) {
            return true;
        }
        return Objects.equals(this.region, other.region);
    }
}