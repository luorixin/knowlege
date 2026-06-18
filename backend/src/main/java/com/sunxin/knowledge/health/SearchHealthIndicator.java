package com.sunxin.knowledge.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Reports the health of the keyword search engine.
 *
 * <p>When the engine is {@code database} (MVP default) the indicator simply reports
 * {@code UP}. When the engine is {@code opensearch} a future implementation will probe
 * the OpenSearch cluster endpoint; the current version always reports {@code UP} with
 * a detail so it never drags down the overall health when OpenSearch is not running.</p>
 */
@Component
public class SearchHealthIndicator implements HealthIndicator {

    private final String engine;

    public SearchHealthIndicator(
            @Value("${knowledge.search.engine:database}") String engine) {
        this.engine = engine;
    }

    @Override
    public Health health() {
        if ("database".equalsIgnoreCase(engine)) {
            return Health.up()
                    .withDetail("engine", engine)
                    .withDetail("note", "uses database keyword search (MVP)")
                    .build();
        }

        // opensearch / future engines: soft probe not yet wired.
        return Health.up()
                .withDetail("engine", engine)
                .withDetail("note", "probe not implemented for engine=" + engine)
                .build();
    }
}
