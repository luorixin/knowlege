package com.sunxin.knowledge.qa.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sunxin.knowledge.qa.dto.AgentChatRequest;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class QuestionUnderstandingService {

    private static final Logger log = LoggerFactory.getLogger(QuestionUnderstandingService.class);
    private final com.sunxin.knowledge.qa.llm.LlmProvider llmProvider;
    
    // Fallback metrics (can be exported via JMX/Actuator in the future)
    private final AtomicInteger fallbackCount = new AtomicInteger(0);
    private final AtomicInteger rewriteFailedCount = new AtomicInteger(0);
    private final AtomicLong totalLatencyMs = new AtomicLong(0);
    private final AtomicInteger rewriteTotalCount = new AtomicInteger(0);

    public QuestionUnderstandingService(com.sunxin.knowledge.qa.llm.LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    public QuestionIntent understand(AgentChatRequest request, java.util.List<com.sunxin.knowledge.qa.llm.ChatMessage> history) {
        String query = request.query().trim();
        long start = System.currentTimeMillis();
        
        com.sunxin.knowledge.qa.llm.QueryRewriteResult result = null;
        try {
            result = llmProvider.rewriteQuery(query, history);
            long latency = System.currentTimeMillis() - start;
            totalLatencyMs.addAndGet(latency);
            rewriteTotalCount.incrementAndGet();
            
            // Check if it fell back to the original query
            if (result == null || result.rewrittenQuery() == null || result.rewrittenQuery().equals(query) || result.rewrittenQuery().isBlank()) {
                fallbackCount.incrementAndGet();
                log.debug("Query rewrite fell back to original query for: {}", query);
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            totalLatencyMs.addAndGet(latency);
            rewriteFailedCount.incrementAndGet();
            fallbackCount.incrementAndGet();
            log.warn("Query rewrite failed, falling back. Latency: {}ms, Error: {}", latency, e.getMessage());
            result = new com.sunxin.knowledge.qa.llm.QueryRewriteResult(query, java.util.List.of());
        }

        return new QuestionIntent(result.rewrittenQuery() == null ? query : result.rewrittenQuery(), request.filters(), result.subQueries() == null ? java.util.List.of() : result.subQueries());
    }
    
    // Metric getters
    public int getFallbackCount() { return fallbackCount.get(); }
    public int getRewriteFailedCount() { return rewriteFailedCount.get(); }
    public long getAverageLatency() { 
        int total = rewriteTotalCount.get();
        return total == 0 ? 0 : totalLatencyMs.get() / total; 
    }
}
