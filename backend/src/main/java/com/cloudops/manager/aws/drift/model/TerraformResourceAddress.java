package com.cloudops.manager.aws.drift.model;

public record TerraformResourceAddress(
    String module,
    String mode,
    String type,
    String name,
    String fullAddress
) {
    public static TerraformResourceAddress of(String module, String mode, String type, String name) {
        String modPrefix = (module != null && !module.isBlank()) ? module + "." : "";
        String full = modPrefix + type + "." + name;
        return new TerraformResourceAddress(module, mode != null ? mode : "managed", type, name, full);
    }
}