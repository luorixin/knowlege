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

    @org.springframework.beans.factory.annotation.Value("${knowledge.ai-service.embedding-model:NOT_SET}")
    private String configuredModel;

    @GetMapping
    public HealthStatus health() {
        return new HealthStatus("knowledge-backend", "UP", RESERVED_MODULES);
    }

    @GetMapping("/model")
    public String getModel() {
        return "Configured Model: " + configuredModel + ", Env Var: " + System.getenv("AI_SERVICE_EMBEDDING_MODEL");
    }
}
