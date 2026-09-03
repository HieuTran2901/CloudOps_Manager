package com.cloudops.manager.operations.impact.service;

import com.cloudops.manager.aws.topology.model.TopologyNodeType;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Deterministic mapper from domain resource references to canonical topology node identifiers.
 */
@Component
public class TopologyResourceIdentityResolver {

    private static final Set<String> UNSUPPORTED_GRAPH_TYPES = Set.of(
            "alb", "application_load_balancer", "load_balancer", "aws::elasticloadbalancingv2::loadbalancer",
            "target_group", "aws::elasticloadbalancingv2::targetgroup",
            "redis", "elasticache", "cache_cluster", "aws::elasticache::cachecluster",
            "s3", "s3_bucket", "bucket", "aws::s3::bucket",
            "cloudwatch", "cloudtrail"
    );

    public Optional<TopologyNodeType> parseNodeType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return Optional.empty();
        }
        String normalized = rawType.trim().toLowerCase();

        if (normalized.equals("ec2") || normalized.equals("ec2_instance") || normalized.equals("i")
                || normalized.equals("instance") || normalized.equals("aws::ec2::instance")) {
            return Optional.of(TopologyNodeType.EC2_INSTANCE);
        }
        if (normalized.equals("rds") || normalized.equals("rds_instance") || normalized.equals("db")
                || normalized.equals("db_instance") || normalized.equals("aws::rds::dbinstance")) {
            return Optional.of(TopologyNodeType.RDS_INSTANCE);
        }
        if (normalized.equals("vpc") || normalized.equals("vpc_resource") || normalized.equals("aws::ec2::vpc")) {
            return Optional.of(TopologyNodeType.VPC);
        }
        if (normalized.equals("subnet") || normalized.equals("subnet_resource") || normalized.equals("aws::ec2::subnet")) {
            return Optional.of(TopologyNodeType.SUBNET);
        }
        if (normalized.equals("security_group") || normalized.equals("sg") || normalized.equals("securitygroup")
                || normalized.equals("aws::ec2::securitygroup")) {
            return Optional.of(TopologyNodeType.SECURITY_GROUP);
        }
        if (normalized.equals("iam_role") || normalized.equals("iam") || normalized.equals("role")
                || normalized.equals("aws::iam::role")) {
            return Optional.of(TopologyNodeType.IAM_ROLE);
        }
        return Optional.empty();
    }

    public boolean isKnownUnsupportedType(String rawType) {
        if (rawType == null || rawType.isBlank()) return false;
        return UNSUPPORTED_GRAPH_TYPES.contains(rawType.trim().toLowerCase());
    }

    public String buildCanonicalNodeId(String accountId, String region, TopologyNodeType type, String resourceId) {
        String safeAcc = (accountId != null && !accountId.isBlank()) ? accountId.trim() : "unknown";
        String safeReg = (region != null && !region.isBlank()) ? region.trim() : "global";
        String safeId = (resourceId != null && !resourceId.isBlank()) ? resourceId.trim() : "unknown";
        return safeAcc + ":" + safeReg + ":" + type.name() + ":" + safeId;
    }
}
