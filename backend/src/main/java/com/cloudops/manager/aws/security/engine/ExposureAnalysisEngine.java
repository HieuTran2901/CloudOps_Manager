package com.cloudops.manager.aws.security.engine;

import com.cloudops.manager.aws.discovery.model.Ec2DetailResource;
import com.cloudops.manager.aws.discovery.model.Ec2NetworkInterfaceDetail;
import com.cloudops.manager.aws.discovery.model.SecurityGroupDetailResource;
import com.cloudops.manager.aws.discovery.model.SecurityGroupRuleDetail;
import com.cloudops.manager.aws.security.model.ExposureStatus;
import com.cloudops.manager.aws.security.model.SecurityExposureResult;
import com.cloudops.manager.aws.topology.model.TopologyNode;
import com.cloudops.manager.aws.topology.model.TopologyNodeType;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ExposureAnalysisEngine {

    private static final List<Integer> ADMINISTRATIVE_PORTS = List.of(22, 3389);

    public List<SecurityExposureResult> evaluateExposures(
            List<Ec2DetailResource> ec2List,
            List<SecurityGroupDetailResource> sgList,
            String accountId,
            String region) {

        if (ec2List == null || sgList == null) {
            return List.of();
        }

        Map<String, SecurityGroupDetailResource> sgMap = new HashMap<>();
        for (SecurityGroupDetailResource sg : sgList) {
            sgMap.put(sg.securityGroupId(), sg);
        }

        List<SecurityExposureResult> results = new ArrayList<>();

        for (Ec2DetailResource ec2 : ec2List) {
            boolean isRunning = "running".equalsIgnoreCase(ec2.instanceState());
            String publicIp = ec2.publicIp();
            String nodeId = TopologyNode.of(TopologyNodeType.EC2_INSTANCE, ec2.instanceId(), accountId, region, Map.of()).nodeId();

            if (isRunning && publicIp != null && !publicIp.isBlank()) {
                boolean isExposed = false;
                Map<String, Object> evidence = new HashMap<>();

                if (ec2.networkInterfaces() != null) {
                    for (Ec2NetworkInterfaceDetail eni : ec2.networkInterfaces()) {
                        if (eni.securityGroupIds() != null) {
                            for (String sgId : eni.securityGroupIds()) {
                                SecurityGroupDetailResource sg = sgMap.get(sgId);
                                if (sg != null && sg.inboundRules() != null) {
                                    for (SecurityGroupRuleDetail rule : sg.inboundRules()) {
                                        if (rule.ipv4Cidrs() != null && rule.ipv4Cidrs().contains("0.0.0.0/0") && coversAdministrativePort(rule)) {
                                            isExposed = true;
                                            evidence.put("instanceId", ec2.instanceId());
                                            evidence.put("publicIp", publicIp);
                                            evidence.put("exposedSecurityGroupId", sgId);
                                            evidence.put("fromPort", rule.fromPort() != null ? rule.fromPort() : "all");
                                            evidence.put("toPort", rule.toPort() != null ? rule.toPort() : "all");
                                            evidence.put("cidr", "0.0.0.0/0");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isExposed) {
                    results.add(new SecurityExposureResult(
                            nodeId, "AWS::EC2::Instance", ec2.instanceId(), ExposureStatus.EXPOSED, evidence, accountId, region
                    ));
                } else {
                    results.add(new SecurityExposureResult(
                            nodeId, "AWS::EC2::Instance", ec2.instanceId(), ExposureStatus.NOT_EXPOSED, Map.of("publicIp", publicIp), accountId, region
                    ));
                }
            }
        }

        return results.stream().sorted(Comparator.comparing(SecurityExposureResult::nodeId)).toList();
    }

    private boolean coversAdministrativePort(SecurityGroupRuleDetail rule) {
        if (rule.fromPort() == null && rule.toPort() == null) return true;
        int from = rule.fromPort() != null ? rule.fromPort() : 0;
        int to = rule.toPort() != null ? rule.toPort() : 65535;
        for (int port : ADMINISTRATIVE_PORTS) {
            if (port >= from && port <= to) return true;
        }
        return false;
    }
}