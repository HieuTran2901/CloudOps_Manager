package com.cloudops.manager.aws.drift.service;

import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.drift.model.*;
import com.cloudops.manager.aws.drift.parser.TerraformStateParser;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class DriftComparisonService {

    private static final Logger log = LoggerFactory.getLogger(DriftComparisonService.class);

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "aws_instance",
            "aws_security_group",
            "aws_db_instance",
            "aws_s3_bucket",
            "aws_vpc"
    );

    private final TerraformStateParser stateParser;
    private final TerraformResourceNormalizer normalizer;
    private final AwsResourceDiscoveryService discoveryService;
    private final AwsIdentityService identityService;

    @Value("${cloudops.aws.region:us-east-1}")
    private String defaultRegion;

    public DriftComparisonService(
            TerraformStateParser stateParser,
            TerraformResourceNormalizer normalizer,
            AwsResourceDiscoveryService discoveryService,
            AwsIdentityService identityService) {
        this.stateParser = stateParser;
        this.normalizer = normalizer;
        this.discoveryService = discoveryService;
        this.identityService = identityService;
    }

    public List<String> getSupportedResourceTypes() {
        return List.copyOf(SUPPORTED_TYPES.stream().sorted().toList());
    }

    public DriftReport evaluateDrift(String terraformStateJson, String optionalRegion) {
        String region = (optionalRegion != null && !optionalRegion.isBlank()) ? optionalRegion.trim() : defaultRegion;
        String accountId = identityService.getCurrentIdentity().accountId();

        TerraformDesiredState desiredState = stateParser.parseStateJson(terraformStateJson);
        InventorySummary inventory = discoveryService.discoverAll(region);

        return compareDesiredVsObserved(desiredState, inventory, accountId, region);
    }

    public DriftReport evaluateCrossAccountDrift(AwsAccountTarget target, String terraformStateJson) {
        String region = (target.region() != null && !target.region().isBlank()) ? target.region().trim() : defaultRegion;

        TerraformDesiredState desiredState = stateParser.parseStateJson(terraformStateJson);
        InventorySummary inventory = discoveryService.discoverAccount(target);

        return compareDesiredVsObserved(desiredState, inventory, target.accountId(), region);
    }

    private DriftReport compareDesiredVsObserved(
            TerraformDesiredState desiredState,
            InventorySummary inventory,
            String accountId,
            String region) {

        List<DriftResourceResult> results = new ArrayList<>();
        int inSync = 0;
        int drifted = 0;
        int notFound = 0;
        int unsupported = 0;
        int insufficient = 0;

        for (TerraformDesiredResource desired : desiredState.resources()) {
            if (!SUPPORTED_TYPES.contains(desired.resourceType().toLowerCase())) {
                unsupported++;
                results.add(new DriftResourceResult(
                        desired.address().fullAddress(), desired.resourceType(), desired.resourceId(),
                        DriftStatus.UNSUPPORTED, List.of(), "Resource type " + desired.resourceType() + " is not currently supported for drift evaluation."
                ));
                continue;
            }

            DriftResourceResult res = evaluateResource(desired, region);
            results.add(res);

            switch (res.status()) {
                case IN_SYNC -> inSync++;
                case DRIFTED -> drifted++;
                case NOT_FOUND -> notFound++;
                case UNSUPPORTED -> unsupported++;
                case INSUFFICIENT_EVIDENCE -> insufficient++;
            }
        }

        return new DriftReport(
                accountId,
                region,
                Instant.now(),
                results.size(),
                inSync,
                drifted,
                notFound,
                unsupported,
                insufficient,
                results
        );
    }

    private DriftResourceResult evaluateResource(TerraformDesiredResource desired, String region) {
        String type = desired.resourceType().toLowerCase();
        String id = desired.resourceId();
        String address = desired.address().fullAddress();

        try {
            switch (type) {
                case "aws_instance" -> {
                    try {
                        Ec2DetailResource observed = discoveryService.getEc2InstanceDetail(id, region);
                        List<DriftAttributeDifference> diffs = normalizer.compareEc2(desired.attributes(), observed);
                        return createResult(address, type, id, diffs);
                    } catch (Exception e) {
                        return new DriftResourceResult(address, type, id, DriftStatus.NOT_FOUND, List.of(), "EC2 instance not found in region " + region);
                    }
                }
                case "aws_security_group" -> {
                    try {
                        SecurityGroupDetailResource observed = discoveryService.getSecurityGroupDetail(id, region);
                        List<DriftAttributeDifference> diffs = normalizer.compareSecurityGroup(desired.attributes(), observed);
                        return createResult(address, type, id, diffs);
                    } catch (Exception e) {
                        return new DriftResourceResult(address, type, id, DriftStatus.NOT_FOUND, List.of(), "Security Group not found in region " + region);
                    }
                }
                case "aws_db_instance" -> {
                    try {
                        RdsDetailResource observed = discoveryService.getRdsInstanceDetail(id, region);
                        List<DriftAttributeDifference> diffs = normalizer.compareRds(desired.attributes(), observed);
                        return createResult(address, type, id, diffs);
                    } catch (Exception e) {
                        return new DriftResourceResult(address, type, id, DriftStatus.NOT_FOUND, List.of(), "RDS instance not found in region " + region);
                    }
                }
                case "aws_s3_bucket" -> {
                    try {
                        S3DetailResource observed = discoveryService.getS3BucketDetail(id, region);
                        List<DriftAttributeDifference> diffs = normalizer.compareS3(desired.attributes(), observed);
                        return createResult(address, type, id, diffs);
                    } catch (Exception e) {
                        return new DriftResourceResult(address, type, id, DriftStatus.NOT_FOUND, List.of(), "S3 bucket not found");
                    }
                }
                case "aws_vpc" -> {
                    try {
                        VpcDetailResource observed = discoveryService.getVpcDetail(id, region);
                        List<DriftAttributeDifference> diffs = normalizer.compareVpc(desired.attributes(), observed);
                        return createResult(address, type, id, diffs);
                    } catch (Exception e) {
                        return new DriftResourceResult(address, type, id, DriftStatus.NOT_FOUND, List.of(), "VPC not found in region " + region);
                    }
                }
                default -> {
                    return new DriftResourceResult(address, type, id, DriftStatus.UNSUPPORTED, List.of(), "Unsupported type");
                }
            }
        } catch (Exception e) {
            return new DriftResourceResult(address, type, id, DriftStatus.INSUFFICIENT_EVIDENCE, List.of(), "Evidence resolution failed: " + e.getMessage());
        }
    }

    private DriftResourceResult createResult(String address, String type, String id, List<DriftAttributeDifference> diffs) {
        if (diffs == null || diffs.isEmpty()) {
            return new DriftResourceResult(address, type, id, DriftStatus.IN_SYNC, List.of(), "Desired state matches live AWS observed evidence.");
        }
        return new DriftResourceResult(address, type, id, DriftStatus.DRIFTED, diffs, diffs.size() + " attribute difference(s) detected.");
    }
}