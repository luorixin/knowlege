package com.sunxin.knowledge.auth.api.dto;

import java.util.List;

public record PolicyDto(
    String id,
    String name,
    String resource,
    List<String> actions,
    Boolean isSystem
) implements java.io.Serializable {}
