package com.sunxin.knowledge.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sunxin.knowledge.persistence.entity.KbEmbeddingIndexTask;
import com.sunxin.knowledge.task.domain.TaskStatus;

public interface KbEmbeddingIndexTaskRepository extends JpaRepository<KbEmbeddingIndexTask, Long> {

    Optional<KbEmbeddingIndexTask> findFirstByStatusOrderByPriorityDescCreatedAtAsc(TaskStatus status);

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
