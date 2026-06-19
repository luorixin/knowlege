package com.sunxin.knowledge.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.document.domain.DocumentStatus;

public interface KbDocumentRepository extends JpaRepository<KbDocument, Long> {

    Page<KbDocument> findBySpaceIdAndStatusNotOrderByCreatedAtDesc(Long spaceId, DocumentStatus status, Pageable pageable);

    Optional<KbDocument> findByIdAndStatusNot(Long id, DocumentStatus status);

    Optional<KbDocument> findFirstByTenantIdAndSpaceIdAndFileHashAndStatusNotOrderByCreatedAtDesc(
            Long tenantId,
            Long spaceId,
            String fileHash,
            DocumentStatus status
    );
}
