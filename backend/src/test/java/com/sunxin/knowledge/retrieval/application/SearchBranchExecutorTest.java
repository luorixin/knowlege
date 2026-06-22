package com.sunxin.knowledge.retrieval.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;

class SearchBranchExecutorTest {

    @Test
    void successfulBranchReturnsItsResults() {
        RetrievalProperties properties = properties(Duration.ofSeconds(1));
        TaskExecutor directExecutor = Runnable::run;
        SearchBranchExecutor executor = new SearchBranchExecutor(directExecutor, properties);

        List<String> results = executor.submit("keyword", "query", () -> List.of("hit")).join();

        assertThat(results).containsExactly("hit");
    }

    @Test
    void failedBranchFallsBackToEmptyResults() {
        RetrievalProperties properties = properties(Duration.ofSeconds(1));
        TaskExecutor directExecutor = Runnable::run;
        SearchBranchExecutor executor = new SearchBranchExecutor(directExecutor, properties);

        List<String> results = executor.<String>submit("vector", "query", () -> {
            throw new IllegalStateException("vector store unavailable");
        }).join();

        assertThat(results).isEmpty();
    }

    @Test
    void timedOutBranchFallsBackWithoutWaitingForProviderCompletion() {
        RetrievalProperties properties = properties(Duration.ofMillis(30));
        TaskExecutor daemonExecutor = command -> {
            Thread thread = new Thread(command, "search-branch-test");
            thread.setDaemon(true);
            thread.start();
        };
        SearchBranchExecutor executor = new SearchBranchExecutor(daemonExecutor, properties);
        long startedAt = System.nanoTime();

        List<String> results = executor.submit("keyword", "slow query", () -> {
            try {
                Thread.sleep(5_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            return List.of("late hit");
        }).join();

        assertThat(results).isEmpty();
        assertThat(Duration.ofNanos(System.nanoTime() - startedAt)).isLessThan(Duration.ofSeconds(1));
    }

    private static RetrievalProperties properties(Duration timeout) {
        RetrievalProperties properties = new RetrievalProperties();
        properties.setBranchTimeout(timeout);
        return properties;
    }
}
