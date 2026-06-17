package com.sunxin.knowledge.common.health;

import java.util.List;

public record HealthStatus(
        String service,
        String status,
        List<String> modules
) {
}
