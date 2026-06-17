package com.sunxin.knowledge.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbDocument;

public interface KbDocumentRepository extends JpaRepository<KbDocument, Long> {

    List<KbDocument> findBySpaceIdAndStatusNotOrderByCreatedAtDesc(Long spaceId, String status);

    Optional<KbDocument> findByIdAndStatusNot(Long id, String status);

    Optional<KbDocument> findFirstByTenantIdAndSpaceIdAndFileHashAndStatusNotOrderByCreatedAtDesc(
            Long tenantId,
            Long spaceId,
            String fileHash,
            String status
    );
}
