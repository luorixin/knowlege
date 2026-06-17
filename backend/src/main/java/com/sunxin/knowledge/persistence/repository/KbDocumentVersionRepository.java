package com.sunxin.knowledge.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;

public interface KbDocumentVersionRepository extends JpaRepository<KbDocumentVersion, Long> {

    Optional<KbDocumentVersion> findByDocIdAndVersionNo(Long docId, Integer versionNo);

    Optional<KbDocumentVersion> findFirstByDocIdAndStatusOrderByVersionNoDesc(Long docId, String status);
}
