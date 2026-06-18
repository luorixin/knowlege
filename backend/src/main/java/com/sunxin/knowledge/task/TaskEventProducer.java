package com.sunxin.knowledge.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TaskEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventProducer.class);

    private final ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider;
    private final boolean enabled;

    public TaskEventProducer(
            ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider,
            @Value("${knowledge.kafka.enabled:true}") boolean enabled
    ) {
        this.kafkaTemplateProvider = kafkaTemplateProvider;
        this.enabled = enabled;
    }

    public void sendParseTask(Long taskId) {
        send(KafkaConfig.TOPIC_PARSE_TASKS, taskId);
    }

    public void sendEmbeddingTask(Long taskId) {
        send(KafkaConfig.TOPIC_EMBEDDING_TASKS, taskId);
    }

    private void send(String topic, Long taskId) {
        if (!enabled) {
            log.debug("Kafka task event publishing is disabled, skip topic={} task_id={}", topic, taskId);
            return;
        }
        KafkaTemplate<String, String> kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        if (kafkaTemplate == null) {
            log.warn("KafkaTemplate is not available, skip topic={} task_id={}", topic, taskId);
            return;
        }
        log.info("Sending task event to Kafka topic={} task_id={}", topic, taskId);
        kafkaTemplate.send(topic, String.valueOf(taskId), String.valueOf(taskId));
    }
}
