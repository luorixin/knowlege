package com.sunxin.knowledge.retrieval;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class RetrievalStartupValidator implements ApplicationRunner {

    private static final Set<String> LOCAL_PROFILES = Set.of("dev", "test", "local");
    private static final Set<String> LOCAL_SEARCH_ENGINES = Set.of("database", "mock");
    private static final Set<String> LOCAL_VECTOR_ENGINES = Set.of("mock");

    private final String searchEngine;
    private final String vectorStoreEngine;
    private final String[] activeProfiles;

    @Autowired
    public RetrievalStartupValidator(
            @Value("${knowledge.search.engine:database}") String searchEngine,
            @Value("${knowledge.vector-store.engine:mock}") String vectorStoreEngine,
            Environment environment
    ) {
        this(searchEngine, vectorStoreEngine, environment.getActiveProfiles());
    }

    RetrievalStartupValidator(String searchEngine, String vectorStoreEngine, String[] activeProfiles) {
        this.searchEngine = normalize(searchEngine);
        this.vectorStoreEngine = normalize(vectorStoreEngine);
        this.activeProfiles = activeProfiles == null ? new String[0] : activeProfiles.clone();
    }

    @Override
    public void run(ApplicationArguments args) {
        validate();
    }

    void validate() {
        if (isLocalProfile()) {
            return;
        }
        if (LOCAL_SEARCH_ENGINES.contains(searchEngine) || LOCAL_VECTOR_ENGINES.contains(vectorStoreEngine)) {
            throw new IllegalStateException("Production retrieval engines must use OpenSearch/Elasticsearch and Milvus; database/mock retrieval engines are local-only");
        }
    }

    private boolean isLocalProfile() {
        Set<String> profiles = Arrays.stream(activeProfiles)
                .map(RetrievalStartupValidator::normalize)
                .collect(Collectors.toSet());
        return profiles.stream().anyMatch(LOCAL_PROFILES::contains);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
