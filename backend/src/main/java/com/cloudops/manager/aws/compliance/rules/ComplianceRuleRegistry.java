package com.cloudops.manager.aws.compliance.rules;

import com.cloudops.manager.aws.compliance.model.ComplianceRule;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ComplianceRuleRegistry {

    private final Map<String, ComplianceRule> ruleMap = new LinkedHashMap<>();

    public ComplianceRuleRegistry(List<ComplianceRule> rules) {
        if (rules != null) {
            for (ComplianceRule rule : rules) {
                ruleMap.put(rule.ruleId(), rule);
            }
        }
    }

    public List<ComplianceRule> getAllRules() {
        return List.copyOf(ruleMap.values());
    }

    public Optional<ComplianceRule> getRule(String ruleId) {
        return Optional.ofNullable(ruleMap.get(ruleId));
    }
}