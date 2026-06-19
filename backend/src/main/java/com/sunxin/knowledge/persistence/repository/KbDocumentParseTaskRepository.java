package com.sunxin.knowledge.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.task.domain.TaskStatus;

public interface KbDocumentParseTaskRepository extends JpaRepository<KbDocumentParseTask, Long> {

    Optional<KbDocumentParseTask> findFirstByDocIdAndVersionIdOrderByCreatedAtDesc(Long docId, Long versionId);

    Optional<KbDocumentParseTask> findFirstByStatusOrderByPriorityDescCreatedAtAsc(TaskStatus status);

    @Query("""
            select task from KbDocumentParseTask task
            where task.spaceId = :spaceId
              and (:status is null or task.status = :status)
            order by task.createdAt desc
            """)
    Page<KbDocumentParseTask> findBySpaceIdAndOptionalStatus(
            @Param("spaceId") Long spaceId,
            @Param("status") TaskStatus status,
            Pageable pageable
    );
}
