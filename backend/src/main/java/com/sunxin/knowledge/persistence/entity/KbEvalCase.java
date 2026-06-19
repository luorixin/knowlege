package com.sunxin.knowledge.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "kb_eval_case")
public class KbEvalCase extends AuditableEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "expected_answer", columnDefinition = "TEXT")
    private String expectedAnswer;

    @Column(name = "expected_doc_ids", length = 1024)
    private String expectedDocIds;

    @Column(name = "tags", length = 512)
    private String tags;

    @Column(name = "case_type", nullable = false, length = 32)
    private String caseType = "QA_RAG";

    @Column(name = "status", nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Long datasetId) {
        this.datasetId = datasetId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getExpectedAnswer() {
        return expectedAnswer;
    }

    public void setExpectedAnswer(String expectedAnswer) {
        this.expectedAnswer = expectedAnswer;
    }

    public String getExpectedDocIds() {
        return expectedDocIds;
    }

    public void setExpectedDocIds(String expectedDocIds) {
        this.expectedDocIds = expectedDocIds;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
