package com.sunxin.knowledge.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbEvalResult;

public interface KbEvalResultRepository extends JpaRepository<KbEvalResult, Long> {

    java.util.List<KbEvalResult> findByRunIdOrderByCreatedAtAsc(String runId);
}
