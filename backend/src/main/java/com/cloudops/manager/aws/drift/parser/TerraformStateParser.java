package com.cloudops.manager.aws.drift.parser;

import com.cloudops.manager.aws.drift.model.TerraformDesiredResource;
import com.cloudops.manager.aws.drift.model.TerraformDesiredState;
import com.cloudops.manager.aws.drift.model.TerraformResourceAddress;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TerraformStateParser {

    private final ObjectMapper objectMapper;

    public TerraformStateParser() {
        this.objectMapper = new ObjectMapper();
    }

    public TerraformStateParser(ObjectMapper objectMapper) {
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    @SuppressWarnings("unchecked")
    public TerraformDesiredState parseStateJson(String jsonContent) {
        if (jsonContent == null || jsonContent.isBlank()) {
            throw new IllegalArgumentException("State content must not be null or blank");
        }

        try {
            JsonNode root = objectMapper.readTree(jsonContent);
            if (!root.isObject()) {
                throw new IllegalArgumentException("State content must be a valid JSON object");
            }

            int version = root.path("version").asInt(4);
            String terraformVersion = root.path("terraform_version").asText("unknown");
            long serial = root.path("serial").asLong(0);

            JsonNode resourcesNode = root.path("resources");
            List<TerraformDesiredResource> desiredResources = new ArrayList<>();

            if (resourcesNode.isArray()) {
                for (JsonNode resNode : resourcesNode) {
                    String module = resNode.path("module").asText(null);
                    String mode = resNode.path("mode").asText("managed");
                    String type = resNode.path("type").asText("");
                    String name = resNode.path("name").asText("");

                    TerraformResourceAddress address = TerraformResourceAddress.of(module, mode, type, name);

                    JsonNode instancesNode = resNode.path("instances");
                    if (instancesNode.isArray()) {
                        for (JsonNode instNode : instancesNode) {
                            JsonNode attrsNode = instNode.path("attributes");
                            Map<String, Object> attributes = objectMapper.convertValue(attrsNode, Map.class);
                            if (attributes == null) {
                                attributes = Map.of();
                            }

                            String resourceId = extractResourceId(attributes, type);
                            String accountId = extractAccountId(attributes);
                            String region = extractRegion(attributes);

                            desiredResources.add(new TerraformDesiredResource(
                                    address, type, resourceId, attributes, accountId, region
                            ));
                        }
                    }
                }
            }

            return new TerraformDesiredState(version, terraformVersion, serial, desiredResources);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse state JSON: " + e.getMessage(), e);
        }
    }

    private String extractResourceId(Map<String, Object> attributes, String type) {
        if (attributes.containsKey("id") && attributes.get("id") != null) {
            return String.valueOf(attributes.get("id"));
        }
        if ("aws_s3_bucket".equalsIgnoreCase(type) && attributes.containsKey("bucket")) {
            return String.valueOf(attributes.get("bucket"));
        }
        if ("aws_db_instance".equalsIgnoreCase(type) && attributes.containsKey("identifier")) {
            return String.valueOf(attributes.get("identifier"));
        }
        return "";
    }

    private String extractAccountId(Map<String, Object> attributes) {
        if (attributes.containsKey("arn")) {
            String arn = String.valueOf(attributes.get("arn"));
            String[] parts = arn.split(":");
            if (parts.length >= 5 && !parts[4].isBlank()) {
                return parts[4];
            }
        }
        if (attributes.containsKey("owner_id")) {
            return String.valueOf(attributes.get("owner_id"));
        }
        return null;
    }

    private String extractRegion(Map<String, Object> attributes) {
        if (attributes.containsKey("arn")) {
            String arn = String.valueOf(attributes.get("arn"));
            String[] parts = arn.split(":");
            if (parts.length >= 4 && !parts[3].isBlank()) {
                return parts[3];
            }
        }
        if (attributes.containsKey("region")) {
            return String.valueOf(attributes.get("region"));
        }
        return null;
    }
}