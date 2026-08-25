package com.cloudops.manager.aws.discovery.service;

import com.cloudops.manager.aws.discovery.config.AwsClientFactory;
import com.cloudops.manager.aws.discovery.model.*;
import com.cloudops.manager.aws.discovery.provider.*;
import com.cloudops.manager.aws.sts.model.AssumeRoleRequest;
import com.cloudops.manager.aws.sts.model.AssumedRoleSession;
import com.cloudops.manager.aws.sts.model.AwsAccountTarget;
import com.cloudops.manager.aws.sts.service.AwsIdentityService;
import com.cloudops.manager.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.rds.RdsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AwsResourceDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(AwsResourceDiscoveryService.class);

    private final AwsIdentityService awsIdentityService;
    private final Ec2Provider ec2Provider;
    private final S3Provider s3Provider;
    private final RdsProvider rdsProvider;
    private final VpcProvider vpcProvider;
    private final SecurityGroupProvider securityGroupProvider;
    private final IamProvider iamProvider;
    private final AwsClientFactory awsClientFactory;

    @Value("${cloudops.aws.region:us-east-1}")
    private String defaultRegion;

    public AwsResourceDiscoveryService(
            AwsIdentityService awsIdentityService,
            Ec2Provider ec2Provider,
            S3Provider s3Provider,
            RdsProvider rdsProvider,
            VpcProvider vpcProvider,
            SecurityGroupProvider securityGroupProvider,
            IamProvider iamProvider,
            AwsClientFactory awsClientFactory) {
        this.awsIdentityService = awsIdentityService;
        this.ec2Provider = ec2Provider;
        this.s3Provider = s3Provider;
        this.rdsProvider = rdsProvider;
        this.vpcProvider = vpcProvider;
        this.securityGroupProvider = securityGroupProvider;
        this.iamProvider = iamProvider;
        this.awsClientFactory = awsClientFactory;
    }

    public InventorySummary discoverAll(String optionalRegion) {
        return performDiscovery(ec2Provider, s3Provider, rdsProvider, vpcProvider, securityGroupProvider,
                resolveEffectiveRegion(optionalRegion), resolveAccountId());
    }

    public InventorySummary discoverAccount(AwsAccountTarget target) {
        String region = resolveEffectiveRegion(target.region());
        log.info("Initiating cross-account discovery for target account: {}, role: {}, region: {}", target.accountId(), target.roleArn(), region);

        AssumedRoleSession session = awsIdentityService.assumeRole(
                new AssumeRoleRequest(target.roleArn(), target.roleSessionName(), target.externalId(), 900)
        );

        try (StsClient sts = awsClientFactory.createStsClient(session, region);
             Ec2Client ec2 = awsClientFactory.createEc2Client(session, region);
             S3Client s3 = awsClientFactory.createS3Client(session, region);
             RdsClient rds = awsClientFactory.createRdsClient(session, region)) {

            String verifiedAccount = sts.getCallerIdentity(GetCallerIdentityRequest.builder().build()).account();
            if (!target.accountId().equals(verifiedAccount)) {
                throw new IllegalStateException("Assumed caller identity account " + verifiedAccount + " does not match requested target account " + target.accountId());
            }

            AwsEc2Provider scopedEc2 = new AwsEc2Provider(ec2);
            AwsS3Provider scopedS3 = new AwsS3Provider(s3);
            AwsRdsProvider scopedRds = new AwsRdsProvider(rds);
            AwsVpcProvider scopedVpc = new AwsVpcProvider(ec2);
            AwsSecurityGroupProvider scopedSg = new AwsSecurityGroupProvider(ec2, rds);

            return performDiscovery(scopedEc2, scopedS3, scopedRds, scopedVpc, scopedSg, region, target.accountId());
        }
    }

    private InventorySummary performDiscovery(
            Ec2Provider ec2, S3Provider s3, RdsProvider rds, VpcProvider vpc, SecurityGroupProvider sg,
            String region, String accountId) {
        List<Ec2InstanceResource> ec2List = ec2.describeInstances(region, accountId);
        List<S3BucketResource> s3List = s3.listBuckets(region, accountId);
        List<RdsInstanceResource> rdsList = rds.describeDbInstances(region, accountId);
        List<VpcResource> vpcList = vpc.describeVpcs(region, accountId);
        List<SecurityGroupResource> sgList = sg.describeSecurityGroups(region, accountId);

        List<CloudResource> all = new ArrayList<>();
        all.addAll(ec2List); all.addAll(s3List); all.addAll(rdsList); all.addAll(vpcList); all.addAll(sgList);

        Map<CloudResourceType, Integer> countByType = new EnumMap<>(CloudResourceType.class);
        countByType.put(CloudResourceType.EC2_INSTANCE, ec2List.size());
        countByType.put(CloudResourceType.S3_BUCKET, s3List.size());
        countByType.put(CloudResourceType.RDS_INSTANCE, rdsList.size());
        countByType.put(CloudResourceType.VPC, vpcList.size());
        countByType.put(CloudResourceType.SECURITY_GROUP, sgList.size());

        return new InventorySummary(accountId, region, all.size(), countByType, all, Instant.now());
    }

    public List<Ec2InstanceResource> getEc2Instances(String optionalRegion) {
        return ec2Provider.describeInstances(resolveEffectiveRegion(optionalRegion), resolveAccountId());
    }

    public Ec2DetailResource getEc2InstanceDetail(String instanceId, String optionalRegion) {
        validateParam("instanceId", instanceId);
        String region = resolveEffectiveRegion(optionalRegion);
        return ec2Provider.getInstance(instanceId.trim(), region, resolveAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("EC2 instance " + instanceId + " not found in region " + region));
    }

    public List<S3BucketResource> getS3Buckets(String optionalRegion) {
        return s3Provider.listBuckets(resolveEffectiveRegion(optionalRegion), resolveAccountId());
    }

    public S3DetailResource getS3BucketDetail(String bucketName, String optionalRegion) {
        validateParam("bucketName", bucketName);
        return s3Provider.getBucket(bucketName.trim(), resolveEffectiveRegion(optionalRegion), resolveAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("S3 bucket " + bucketName + " not found"));
    }

    public List<RdsInstanceResource> getRdsInstances(String optionalRegion) {
        return rdsProvider.describeDbInstances(resolveEffectiveRegion(optionalRegion), resolveAccountId());
    }

    public RdsDetailResource getRdsInstanceDetail(String dbInstanceIdentifier, String optionalRegion) {
        validateParam("dbInstanceIdentifier", dbInstanceIdentifier);
        String region = resolveEffectiveRegion(optionalRegion);
        return rdsProvider.getDbInstance(dbInstanceIdentifier.trim(), region, resolveAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("RDS instance " + dbInstanceIdentifier + " not found in region " + region));
    }

    public List<VpcResource> getVpcs(String optionalRegion) {
        return vpcProvider.describeVpcs(resolveEffectiveRegion(optionalRegion), resolveAccountId());
    }

    public VpcDetailResource getVpcDetail(String vpcId, String optionalRegion) {
        validateParam("vpcId", vpcId);
        String region = resolveEffectiveRegion(optionalRegion);
        return vpcProvider.getVpc(vpcId.trim(), region, resolveAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("VPC " + vpcId + " not found in region " + region));
    }

    public VpcTopologyResource getVpcTopology(String vpcId, String optionalRegion) {
        validateParam("vpcId", vpcId);
        String region = resolveEffectiveRegion(optionalRegion);
        return vpcProvider.getVpcTopology(vpcId.trim(), region, resolveAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("VPC " + vpcId + " not found in region " + region));
    }

    public List<SecurityGroupResource> getSecurityGroups(String optionalRegion) {
        return securityGroupProvider.describeSecurityGroups(resolveEffectiveRegion(optionalRegion), resolveAccountId());
    }

    public SecurityGroupDetailResource getSecurityGroupDetail(String securityGroupId, String optionalRegion) {
        validateParam("securityGroupId", securityGroupId);
        String region = resolveEffectiveRegion(optionalRegion);
        return securityGroupProvider.getSecurityGroup(securityGroupId.trim(), region, resolveAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Security Group " + securityGroupId + " not found in region " + region));
    }

    public SecurityGroupTopologyResource getSecurityGroupTopology(String securityGroupId, String optionalRegion) {
        validateParam("securityGroupId", securityGroupId);
        String region = resolveEffectiveRegion(optionalRegion);
        return securityGroupProvider.getSecurityGroupTopology(securityGroupId.trim(), region, resolveAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Security Group " + securityGroupId + " not found in region " + region));
    }

    public List<IamUserResource> getIamUsers() {
        return iamProvider.listUsers(resolveAccountId());
    }

    public IamUserDetailResource getIamUserDetail(String userName) {
        validateParam("userName", userName);
        return iamProvider.getUser(userName.trim(), resolveAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("IAM user " + userName + " not found"));
    }

    public List<IamRoleResource> getIamRoles() {
        return iamProvider.listRoles(resolveAccountId());
    }

    public IamRoleDetailResource getIamRoleDetail(String roleName) {
        validateParam("roleName", roleName);
        return iamProvider.getRole(roleName.trim(), resolveAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("IAM role " + roleName + " not found"));
    }

    public List<IamInstanceProfileInfo> getIamRoleInstanceProfiles(String roleName) {
        validateParam("roleName", roleName);
        return iamProvider.getInstanceProfilesForRole(roleName.trim(), resolveAccountId());
    }

    public IamPolicyDetailResource getIamPolicyDetail(String policyArn) {
        validateParam("policyArn", policyArn);
        return iamProvider.getPolicy(policyArn.trim(), resolveAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("IAM policy " + policyArn + " not found"));
    }

    public IamIdentityTopologyResource getIamTopology() {
        String accountId = resolveAccountId();
        return new IamIdentityTopologyResource(iamProvider.listUsers(accountId), iamProvider.listRoles(accountId), Instant.now());
    }

    public String resolveEffectiveRegion(String optionalRegion) {
        return (optionalRegion != null && !optionalRegion.isBlank()) ? optionalRegion.trim() : defaultRegion;
    }

    public String resolveAccountId() {
        return awsIdentityService.getCurrentIdentity().accountId();
    }

    private void validateParam(String name, String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be null or blank");
    }
}