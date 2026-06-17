package com.sunxin.knowledge.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbEmbeddingIndexTask;

public interface KbEmbeddingIndexTaskRepository extends JpaRepository<KbEmbeddingIndexTask, Long> {
}
