package com.sunxin.knowledge.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.persistence.entity.KbEmbeddingIndexTask;
import com.sunxin.knowledge.task.domain.TaskStatus;

public interface KbEmbeddingIndexTaskRepository extends JpaRepository<KbEmbeddingIndexTask, Long> {

    Optional<KbEmbeddingIndexTask> findFirstByStatusOrderByPriorityDescCreatedAtAsc(TaskStatus status);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update KbEmbeddingIndexTask task
               set task.status = :runningStatus,
                   task.progressPercent = :progressPercent,
                   task.startedAt = :startedAt,
                   task.finishedAt = null,
                   task.errorCode = null,
                   task.errorMessage = null
             where task.id = :taskId
               and task.status = :pendingStatus
            """)
    int claimPending(
            @Param("taskId") Long taskId,
            @Param("pendingStatus") TaskStatus pendingStatus,
            @Param("runningStatus") TaskStatus runningStatus,
            @Param("progressPercent") int progressPercent,
            @Param("startedAt") java.time.LocalDateTime startedAt
    );

    @Query("""
            select task from KbEmbeddingIndexTask task
            where task.spaceId = :spaceId
              and (:status is null or task.status = :status)
            order by task.createdAt desc
            """)
    Page<KbEmbeddingIndexTask> findBySpaceIdAndOptionalStatus(
            @Param("spaceId") Long spaceId,
            @Param("status") TaskStatus status,
            Pageable pageable
    );

    @Modifying
    @Query("delete from KbEmbeddingIndexTask task where task.versionId = :versionId")
    int deleteByVersionId(@Param("versionId") Long versionId);
}
