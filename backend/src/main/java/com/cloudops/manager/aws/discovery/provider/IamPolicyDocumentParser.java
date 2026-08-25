package com.cloudops.manager.aws.discovery.provider;

import com.cloudops.manager.aws.discovery.model.IamPolicyStatement;
import com.cloudops.manager.aws.discovery.model.IamTrustStatement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class IamPolicyDocumentParser {

    private static final Logger log = LoggerFactory.getLogger(IamPolicyDocumentParser.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private IamPolicyDocumentParser() {}

    public static List<IamPolicyStatement> parsePolicyStatements(String policyDocument) {
        if (policyDocument == null || policyDocument.isBlank()) {
            return Collections.emptyList();
        }
        try {
            String decoded = decodeIfNecessary(policyDocument);
            JsonNode root = MAPPER.readTree(decoded);
            JsonNode stmtNode = root.get("Statement");
            if (stmtNode == null) return Collections.emptyList();

            List<IamPolicyStatement> list = new ArrayList<>();
            if (stmtNode.isArray()) {
                for (JsonNode s : stmtNode) list.add(mapStatement(s));
            } else if (stmtNode.isObject()) {
                list.add(mapStatement(stmtNode));
            }
            return list;
        } catch (Exception e) {
            log.debug("Failed to parse policy document: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<IamTrustStatement> parseTrustStatements(String policyDocument) {
        if (policyDocument == null || policyDocument.isBlank()) {
            return Collections.emptyList();
        }
        try {
            String decoded = decodeIfNecessary(policyDocument);
            JsonNode root = MAPPER.readTree(decoded);
            JsonNode stmtNode = root.get("Statement");
            if (stmtNode == null) return Collections.emptyList();

            List<IamTrustStatement> list = new ArrayList<>();
            if (stmtNode.isArray()) {
                for (JsonNode s : stmtNode) list.add(mapTrustStatement(s));
            } else if (stmtNode.isObject()) {
                list.add(mapTrustStatement(stmtNode));
            }
            return list;
        } catch (Exception e) {
            log.debug("Failed to parse trust policy document: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private static IamPolicyStatement mapStatement(JsonNode s) {
        String effect = s.path("Effect").asText("Allow");
        List<String> actions = extractStringOrArray(s.get("Action"));
        List<String> notActions = extractStringOrArray(s.get("NotAction"));
        List<String> resources = extractStringOrArray(s.get("Resource"));
        List<String> notResources = extractStringOrArray(s.get("NotResource"));
        @SuppressWarnings("unchecked")
        Map<String, Object> condition = s.has("Condition") ? MAPPER.convertValue(s.get("Condition"), Map.class) : null;
        return new IamPolicyStatement(effect, actions, notActions, resources, notResources, condition);
    }

    private static IamTrustStatement mapTrustStatement(JsonNode s) {
        String effect = s.path("Effect").asText("Allow");
        List<String> principals = new ArrayList<>();
        JsonNode pNode = s.get("Principal");
        if (pNode != null) {
            if (pNode.isTextual()) {
                principals.add(pNode.asText());
            } else if (pNode.isObject()) {
                pNode.fields().forEachRemaining(entry -> {
                    if (entry.getValue().isArray()) {
                        for (JsonNode item : entry.getValue()) principals.add(entry.getKey() + ":" + item.asText());
                    } else {
                        principals.add(entry.getKey() + ":" + entry.getValue().asText());
                    }
                });
            }
        }
        String action = s.path("Action").asText("sts:AssumeRole");
        @SuppressWarnings("unchecked")
        Map<String, Object> condition = s.has("Condition") ? MAPPER.convertValue(s.get("Condition"), Map.class) : null;
        return new IamTrustStatement(effect, principals, action, condition);
    }

    private static List<String> extractStringOrArray(JsonNode node) {
        if (node == null) return Collections.emptyList();
        if (node.isTextual()) return List.of(node.asText());
        if (node.isArray()) {
            List<String> list = new ArrayList<>();
            for (JsonNode n : node) list.add(n.asText());
            return list;
        }
        return Collections.emptyList();
    }

    private static String decodeIfNecessary(String input) {
        if (input.startsWith("%7B") || input.contains("%22")) {
            return URLDecoder.decode(input, StandardCharsets.UTF_8);
        }
        return input;
    }
}