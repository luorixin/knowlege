package com.sunxin.knowledge.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class DatabaseMigrationTest {

    private static final List<String> EXPECTED_TABLES = List.of(
            "kb_space",
            "kb_document",
            "kb_document_version",
            "kb_document_chunk",
            "kb_desensitization_mapping",
            "kb_document_parse_task",
            "kb_embedding_index_task",
            "kb_user",
            "kb_role",
            "kb_user_role",
            "kb_permission_policy",
            "kb_query_session",
            "kb_query_message",
            "kb_answer_citation",
            "kb_audit_log",
            "kb_eval_dataset",
            "kb_eval_case",
            "kb_eval_result"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesCoreKnowledgeTables() {
        String placeholders = IntStream.range(0, EXPECTED_TABLES.size())
                .mapToObj(index -> "?")
                .collect(Collectors.joining(", "));

        Integer tableCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND LOWER(TABLE_NAME) IN (%s)
                """.formatted(placeholders), Integer.class, EXPECTED_TABLES.toArray());

        assertThat(tableCount).isEqualTo(EXPECTED_TABLES.size());
    }

    @Test
    void chunkTableKeepsRetrievalAndCitationFields() {
        List<String> columns = jdbcTemplate.queryForList("""
                SELECT LOWER(COLUMN_NAME)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND LOWER(TABLE_NAME) = 'kb_document_chunk'
                """, String.class);

        assertThat(columns).contains(
                "doc_id",
                "version_id",
                "chunk_index",
                "page_no",
                "section_title",
                "content",
                "token_count",
                "metadata_json"
        );
    }
}
