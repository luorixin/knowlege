package com.sunxin.knowledge.common.health;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private static final List<String> RESERVED_MODULES = List.of(
            "auth",
            "knowledge-base",
            "document",
            "retrieval",
            "qa",
            "task",
            "audit"
    );

    @GetMapping
    public HealthStatus health() {
        return new HealthStatus("knowledge-backend", "UP", RESERVED_MODULES);
    }
}
