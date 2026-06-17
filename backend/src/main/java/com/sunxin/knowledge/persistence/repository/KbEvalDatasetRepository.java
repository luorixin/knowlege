package com.sunxin.knowledge.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbEvalDataset;

public interface KbEvalDatasetRepository extends JpaRepository<KbEvalDataset, Long> {

    java.util.Optional<KbEvalDataset> findByIdAndStatus(Long id, String status);
}
