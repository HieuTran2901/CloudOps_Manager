package com.cloudops.manager.aws.discovery.controller;

import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.aws.discovery.service.AwsResourceDiscoveryService;
import com.cloudops.manager.aws.observability.model.MetricSeries;
import com.cloudops.manager.aws.observability.service.AwsObservabilityService;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.common.api.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/aws/resources")
public class AwsResourceDiscoveryController {

    private final AwsResourceDiscoveryService discoveryService;
    private final AwsObservabilityService observabilityService;

    public AwsResourceDiscoveryController(AwsResourceDiscoveryService discoveryService, AwsObservabilityService observabilityService) {
        this.discoveryService = discoveryService;
        this.observabilityService = observabilityService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<InventorySummary>> getInventorySummary(@RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.discoverAll(region), "Inventory discovery completed successfully."));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<ApiResponse<InventorySummary>> discoverCrossAccount(
            @PathVariable String accountId,
            @RequestParam String roleArn,
            @RequestParam(required = false) String roleSessionName,
            @RequestParam(required = false) String externalId,
            @RequestParam(required = false) String region) {
        AwsAccountTarget target = new AwsAccountTarget(accountId, roleArn, roleSessionName, externalId, region);
        InventorySummary summary = discoveryService.discoverAccount(target);
        return ResponseEntity.ok(ApiResponse.success(summary, "Cross-account inventory discovery completed successfully."));
    }

    @GetMapping("/ec2")
    public ResponseEntity<ApiResponse<List<Ec2InstanceResource>>> getEc2Instances(@RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getEc2Instances(region), "EC2 instances retrieved successfully."));
    }

    @GetMapping("/ec2/{instanceId}")
    public ResponseEntity<ApiResponse<Ec2DetailResource>> getEc2InstanceDetail(@PathVariable String instanceId, @RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getEc2InstanceDetail(instanceId, region), "EC2 instance details retrieved successfully."));
    }

    @GetMapping("/ec2/{instanceId}/metrics")
    public ResponseEntity<ApiResponse<MetricSeries>> getEc2Metrics(
            @PathVariable String instanceId, @RequestParam String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(required = false, defaultValue = "300") Integer period,
            @RequestParam(required = false, defaultValue = "Average") String statistic,
            @RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(observabilityService.getEc2Metric(instanceId, metric, startTime, endTime, period, statistic, region), "EC2 metrics retrieved successfully."));
    }

    @GetMapping("/s3")
    public ResponseEntity<ApiResponse<List<S3BucketResource>>> getS3Buckets(@RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getS3Buckets(region), "S3 buckets retrieved successfully."));
    }

    @GetMapping("/s3/{bucketName}")
    public ResponseEntity<ApiResponse<S3DetailResource>> getS3BucketDetail(@PathVariable String bucketName, @RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getS3BucketDetail(bucketName, region), "S3 bucket details retrieved successfully."));
    }

    @GetMapping("/rds")
    public ResponseEntity<ApiResponse<List<RdsInstanceResource>>> getRdsInstances(@RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getRdsInstances(region), "RDS instances retrieved successfully."));
    }

    @GetMapping("/rds/{dbInstanceIdentifier}")
    public ResponseEntity<ApiResponse<RdsDetailResource>> getRdsInstanceDetail(@PathVariable String dbInstanceIdentifier, @RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getRdsInstanceDetail(dbInstanceIdentifier, region), "RDS instance details retrieved successfully."));
    }

    @GetMapping("/rds/{dbInstanceIdentifier}/metrics")
    public ResponseEntity<ApiResponse<MetricSeries>> getRdsMetrics(
            @PathVariable String dbInstanceIdentifier, @RequestParam String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(required = false, defaultValue = "300") Integer period,
            @RequestParam(required = false, defaultValue = "Average") String statistic,
            @RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(observabilityService.getRdsMetric(dbInstanceIdentifier, metric, startTime, endTime, period, statistic, region), "RDS metrics retrieved successfully."));
    }

    @GetMapping("/vpcs")
    public ResponseEntity<ApiResponse<List<VpcResource>>> getVpcs(@RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getVpcs(region), "VPCs retrieved successfully."));
    }

    @GetMapping("/vpcs/{vpcId}")
    public ResponseEntity<ApiResponse<VpcDetailResource>> getVpcDetail(@PathVariable String vpcId, @RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getVpcDetail(vpcId, region), "VPC details retrieved successfully."));
    }

    @GetMapping("/vpcs/{vpcId}/topology")
    public ResponseEntity<ApiResponse<VpcTopologyResource>> getVpcTopology(@PathVariable String vpcId, @RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getVpcTopology(vpcId, region), "VPC topology retrieved successfully."));
    }

    @GetMapping("/security-groups")
    public ResponseEntity<ApiResponse<List<SecurityGroupResource>>> getSecurityGroups(@RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getSecurityGroups(region), "Security Groups retrieved successfully."));
    }

    @GetMapping("/security-groups/{securityGroupId}")
    public ResponseEntity<ApiResponse<SecurityGroupDetailResource>> getSecurityGroupDetail(@PathVariable String securityGroupId, @RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getSecurityGroupDetail(securityGroupId, region), "Security Group details retrieved successfully."));
    }

    @GetMapping("/security-groups/{securityGroupId}/topology")
    public ResponseEntity<ApiResponse<SecurityGroupTopologyResource>> getSecurityGroupTopology(@PathVariable String securityGroupId, @RequestParam(required = false) String region) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getSecurityGroupTopology(securityGroupId, region), "Security Group topology retrieved successfully."));
    }

    @GetMapping("/iam/users")
    public ResponseEntity<ApiResponse<List<IamUserResource>>> getIamUsers() {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getIamUsers(), "IAM users retrieved successfully."));
    }

    @GetMapping("/iam/users/{userName}")
    public ResponseEntity<ApiResponse<IamUserDetailResource>> getIamUserDetail(@PathVariable String userName) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getIamUserDetail(userName), "IAM user details retrieved successfully."));
    }

    @GetMapping("/iam/roles")
    public ResponseEntity<ApiResponse<List<IamRoleResource>>> getIamRoles() {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getIamRoles(), "IAM roles retrieved successfully."));
    }

    @GetMapping("/iam/roles/{roleName}")
    public ResponseEntity<ApiResponse<IamRoleDetailResource>> getIamRoleDetail(@PathVariable String roleName) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getIamRoleDetail(roleName), "IAM role details retrieved successfully."));
    }

    @GetMapping("/iam/roles/{roleName}/instance-profiles")
    public ResponseEntity<ApiResponse<List<IamInstanceProfileInfo>>> getIamRoleInstanceProfiles(@PathVariable String roleName) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getIamRoleInstanceProfiles(roleName), "IAM role instance profiles retrieved successfully."));
    }

    @GetMapping("/iam/policies")
    public ResponseEntity<ApiResponse<IamPolicyDetailResource>> getIamPolicyDetail(@RequestParam String policyArn) {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getIamPolicyDetail(policyArn), "IAM policy details retrieved successfully."));
    }

    @GetMapping("/iam/topology")
    public ResponseEntity<ApiResponse<IamIdentityTopologyResource>> getIamTopology() {
        return ResponseEntity.ok(ApiResponse.success(discoveryService.getIamTopology(), "IAM topology retrieved successfully."));
    }
}