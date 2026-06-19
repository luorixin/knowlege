package com.sunxin.knowledge.auth.api.dto;

import java.util.List;

public record RoleDto(
    String id,
    String name,
    String description,
    Integer memberCount,
    Integer policyCount,
    Boolean isSystem
) implements java.io.Serializable {}
