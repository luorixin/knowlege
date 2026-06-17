package com.sunxin.knowledge.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbEvalCase;

public interface KbEvalCaseRepository extends JpaRepository<KbEvalCase, Long> {

    java.util.List<KbEvalCase> findByDatasetIdAndStatusOrderByCreatedAtAsc(Long datasetId, String status);
}
