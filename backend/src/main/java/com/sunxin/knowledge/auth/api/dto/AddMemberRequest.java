package com.sunxin.knowledge.auth.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AddMemberRequest(
    @NotBlank String username,
    @NotBlank String roleId
) {}
