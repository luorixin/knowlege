package com.sunxin.knowledge.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports the health of the vector store engine.
 *
 * <p>When the engine is {@code mock} (MVP default) the indicator simply reports
 * {@code UP}. When the engine is {@code milvus} a future implementation will probe
 * the Milvus endpoint; the current version always reports {@code UP} with a detail
 * so it never drags down the overall health when Milvus is not running.</p>
 */
@Component
public class VectorHealthIndicator implements HealthIndicator {

    private final String engine;

    public VectorHealthIndicator(
            @Value("${knowledge.vector-store.engine:mock}") String engine) {
        this.engine = engine;
    }

    @Override
    public Health health() {
        if ("mock".equalsIgnoreCase(engine)) {
            return Health.up()
                    .withDetail("engine", engine)
                    .withDetail("note", "uses in-database mock vector search (MVP)")
                    .build();
        }

        // milvus / future engines: soft probe not yet wired.
        return Health.up()
                .withDetail("engine", engine)
                .withDetail("note", "probe not implemented for engine=" + engine)
                .build();
    }
}
