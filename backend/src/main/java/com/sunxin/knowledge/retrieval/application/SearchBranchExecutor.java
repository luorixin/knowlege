package com.sunxin.knowledge.retrieval.application;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
@EnableConfigurationProperties(RetrievalProperties.class)
public class SearchBranchExecutor {

    private static final Logger log = LoggerFactory.getLogger(SearchBranchExecutor.class);

    private final TaskExecutor taskExecutor;
    private final Duration timeout;

    public SearchBranchExecutor(
            @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
            RetrievalProperties properties
    ) {
        this.taskExecutor = taskExecutor;
        this.timeout = properties.getBranchTimeout();
    }

    public <T> CompletableFuture<List<T>> submit(
            String branch,
            String query,
            Supplier<List<T>> searchOperation
    ) {
        try {
            return CompletableFuture.supplyAsync(searchOperation, taskExecutor)
                    .orTimeout(Math.max(1L, timeout.toMillis()), TimeUnit.MILLISECONDS)
                    .exceptionally(error -> fallback(branch, query, error));
        } catch (RuntimeException ex) {
            return CompletableFuture.completedFuture(fallback(branch, query, ex));
        }
    }

    private <T> List<T> fallback(String branch, String query, Throwable error) {
        Throwable cause = unwrap(error);
        log.warn(
                "retrieval_branch_fallback branch={} query_fingerprint={} timeout_ms={} error_code={}",
                branch,
                fingerprint(query),
                timeout.toMillis(),
                cause.getClass().getSimpleName()
        );
        return List.of();
    }

    private static Throwable unwrap(Throwable error) {
        if (error instanceof CompletionException && error.getCause() != null) {
            return error.getCause();
        }
        return error;
    }

    private static String fingerprint(String query) {
        return query == null ? "null" : Integer.toHexString(query.hashCode());
    }
}
