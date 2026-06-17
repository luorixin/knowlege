package com.sunxin.knowledge.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;

public interface KbDocumentParseTaskRepository extends JpaRepository<KbDocumentParseTask, Long> {

    Optional<KbDocumentParseTask> findFirstByDocIdAndVersionIdOrderByCreatedAtDesc(Long docId, Long versionId);
}
