package com.sunxin.knowledge.health;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Pings the Python AI service health endpoint.
 *
 * <p>Always reports {@code UP} so it never drags down the overall health when the
 * AI service is not started (e.g. local backend-only development). The actual status
 * is recorded in the {@code detail} map.</p>
 */
@Component
public class AiServiceHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(AiServiceHealthIndicator.class);

    private final String healthUrl;

    public AiServiceHealthIndicator(
            @Value("${knowledge.ai-service.endpoint:http://localhost:8001}") String endpoint) {
        this.healthUrl = endpoint.endsWith("/") ? endpoint + "api/v1/health" : endpoint + "/api/v1/health";
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.up().withDetail("endpoint", healthUrl);

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(3))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                builder.withDetail("status", "reachable")
                        .withDetail("httpStatus", response.statusCode());
            } else {
                builder.withDetail("status", "unexpected")
                        .withDetail("httpStatus", response.statusCode())
                        .withDetail("body", response.body());
            }
        } catch (Exception ex) {
            log.debug("AI service health probe failed: endpoint={}", healthUrl, ex);
            builder.withDetail("status", "unreachable")
                    .withDetail("error", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }

        return builder.build();
    }
}
