package com.cloudops.manager.aws.quota.service;

import com.cloudops.manager.aws.discovery.model.Ec2InstanceResource;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authoritative resolver and aggregator for EC2 instance vCPU capacity.
 */
public final class Ec2VcpuResolver {

    private static final Pattern MULTI_XLARGE_PATTERN = Pattern.compile("^[a-z0-9]+\\.([0-9]+)xlarge$");

    private static final Map<String, Integer> KNOWN_EXACT_VCPUS = Map.ofEntries(
            Map.entry("nano", 2),
            Map.entry("micro", 2),
            Map.entry("small", 2),
            Map.entry("medium", 2),
            Map.entry("large", 2),
            Map.entry("xlarge", 4),
            Map.entry("2xlarge", 8),
            Map.entry("3xlarge", 12),
            Map.entry("4xlarge", 16),
            Map.entry("6xlarge", 24),
            Map.entry("8xlarge", 32),
            Map.entry("9xlarge", 36),
            Map.entry("12xlarge", 48),
            Map.entry("16xlarge", 64),
            Map.entry("18xlarge", 72),
            Map.entry("24xlarge", 96),
            Map.entry("32xlarge", 128),
            Map.entry("48xlarge", 192),
            Map.entry("metal", 96)
    );

    // Specific older generation overrides where nano/micro/small had 1 vCPU
    private static final Map<String, Integer> SPECIFIC_OVERRIDES = Map.ofEntries(
            Map.entry("t2.nano", 1),
            Map.entry("t2.micro", 1),
            Map.entry("t2.small", 1),
            Map.entry("t1.micro", 1),
            Map.entry("c5.large", 2),
            Map.entry("c5.xlarge", 4),
            Map.entry("c5.2xlarge", 8),
            Map.entry("c5.4xlarge", 16),
            Map.entry("m5.large", 2),
            Map.entry("m5.xlarge", 4),
            Map.entry("m5.2xlarge", 8),
            Map.entry("m5.4xlarge", 16),
            Map.entry("r5.large", 2),
            Map.entry("r5.xlarge", 4),
            Map.entry("r5.2xlarge", 8),
            Map.entry("r5.4xlarge", 16)
    );

    private Ec2VcpuResolver() {}

    public static int resolveVcpus(String instanceType) {
        if (instanceType == null || instanceType.isBlank()) {
            return 0;
        }

        String normalized = instanceType.toLowerCase().trim();

        if (SPECIFIC_OVERRIDES.containsKey(normalized)) {
            return SPECIFIC_OVERRIDES.get(normalized);
        }

        int dotIndex = normalized.indexOf('.');
        if (dotIndex != -1 && dotIndex < normalized.length() - 1) {
            String sizePart = normalized.substring(dotIndex + 1);

            if (KNOWN_EXACT_VCPUS.containsKey(sizePart)) {
                return KNOWN_EXACT_VCPUS.get(sizePart);
            }

            Matcher matcher = MULTI_XLARGE_PATTERN.matcher(normalized);
            if (matcher.matches()) {
                try {
                    int multiplier = Integer.parseInt(matcher.group(1));
                    return multiplier * 4;
                } catch (NumberFormatException ignored) {}
            }
        }

        return 2;
    }

    public static double sumTotalVcpus(List<Ec2InstanceResource> instances) {
        if (instances == null || instances.isEmpty()) {
            return 0.0;
        }

        double total = 0.0;
        for (Ec2InstanceResource instance : instances) {
            if (instance == null) continue;
            String status = instance.status() != null ? instance.status().toLowerCase() : "";
            if (!"terminated".equals(status) && !"shutting-down".equals(status)) {
                total += resolveVcpus(instance.instanceType());
            }
        }
        return total;
    }
}
