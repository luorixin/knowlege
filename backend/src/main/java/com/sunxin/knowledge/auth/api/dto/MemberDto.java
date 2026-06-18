package com.sunxin.knowledge.auth.api.dto;

public record MemberDto(
    String id,
    String username,
    String displayName,
    String role,
    String status
) {}
