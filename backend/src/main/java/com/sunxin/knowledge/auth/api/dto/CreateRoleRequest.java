package com.sunxin.knowledge.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(
    @NotBlank String name,
    String description
) {}
