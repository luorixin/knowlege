package com.sunxin.knowledge.persistence.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "kb_eval_result")
public class KbEvalResult extends AuditableEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "dataset_id", nullable = false)
    private Long datasetId;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "run_id", nullable = false, length = 128)
    private String runId;

    @Column(name = "query_session_id")
    private Long querySessionId;

    @Column(name = "answer_message_id")
    private Long answerMessageId;

    @Column(name = "actual_answer", columnDefinition = "TEXT")
    private String actualAnswer;

    @Column(name = "score", precision = 10, scale = 6)
    private BigDecimal score;

    @Column(name = "hit_count", nullable = false)
    private Integer hitCount = 0;

    @Column(name = "citation_hit_count", nullable = false)
    private Integer citationHitCount = 0;

    @Column(name = "evaluator_type", length = 64)
    private String evaluatorType;

    @Column(name = "evaluator_model", length = 256)
    private String evaluatorModel;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "COMPLETED";

    @Column(name = "detail_json", columnDefinition = "TEXT")
    private String detailJson;

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

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public Long getQuerySessionId() {
        return querySessionId;
    }

    public void setQuerySessionId(Long querySessionId) {
        this.querySessionId = querySessionId;
    }

    public Long getAnswerMessageId() {
        return answerMessageId;
    }

    public void setAnswerMessageId(Long answerMessageId) {
        this.answerMessageId = answerMessageId;
    }

    public String getActualAnswer() {
        return actualAnswer;
    }

    public void setActualAnswer(String actualAnswer) {
        this.actualAnswer = actualAnswer;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Integer getHitCount() {
        return hitCount;
    }

    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
    }

    public Integer getCitationHitCount() {
        return citationHitCount;
    }

    public void setCitationHitCount(Integer citationHitCount) {
        this.citationHitCount = citationHitCount;
    }

    public String getEvaluatorType() {
        return evaluatorType;
    }

    public void setEvaluatorType(String evaluatorType) {
        this.evaluatorType = evaluatorType;
    }

    public String getEvaluatorModel() {
        return evaluatorModel;
    }

    public void setEvaluatorModel(String evaluatorModel) {
        this.evaluatorModel = evaluatorModel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }
}
