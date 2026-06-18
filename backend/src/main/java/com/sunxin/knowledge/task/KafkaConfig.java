package com.sunxin.knowledge.task;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@Profile("!test")
public class KafkaConfig {
    
    public static final String TOPIC_PARSE_TASKS = "knowledge_parse_tasks";
    public static final String TOPIC_EMBEDDING_TASKS = "knowledge_embedding_tasks";

    @Bean
    public NewTopic parseTasksTopic() {
        return TopicBuilder.name(TOPIC_PARSE_TASKS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic embeddingTasksTopic() {
        return TopicBuilder.name(TOPIC_EMBEDDING_TASKS)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
