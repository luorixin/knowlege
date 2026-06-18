package com.sunxin.knowledge.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;

public interface KbDocumentParseTaskRepository extends JpaRepository<KbDocumentParseTask, Long> {

    Optional<KbDocumentParseTask> findFirstByDocIdAndVersionIdOrderByCreatedAtDesc(Long docId, Long versionId);

    Optional<KbDocumentParseTask> findFirstByStatusOrderByPriorityDescCreatedAtAsc(String status);

    @Query("""
            select task from KbDocumentParseTask task
            where task.spaceId = :spaceId
              and (:status is null or task.status = :status)
            order by task.createdAt desc
            """)
    List<KbDocumentParseTask> findBySpaceIdAndOptionalStatus(
            @Param("spaceId") Long spaceId,
            @Param("status") String status,
            Pageable pageable
    );
}
