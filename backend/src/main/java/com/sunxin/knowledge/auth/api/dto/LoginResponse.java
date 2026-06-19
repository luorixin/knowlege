package com.sunxin.knowledge.auth.api.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
