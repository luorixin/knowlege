package com.sunxin.knowledge.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sunxin.knowledge.persistence.entity.KbEmbeddingIndexTask;

public interface KbEmbeddingIndexTaskRepository extends JpaRepository<KbEmbeddingIndexTask, Long> {

    Optional<KbEmbeddingIndexTask> findFirstByStatusOrderByPriorityDescCreatedAtAsc(String status);

    @Query("""
            select task from KbEmbeddingIndexTask task
            where task.spaceId = :spaceId
              and (:status is null or task.status = :status)
            order by task.createdAt desc
            """)
    List<KbEmbeddingIndexTask> findBySpaceIdAndOptionalStatus(
            @Param("spaceId") Long spaceId,
            @Param("status") String status,
            Pageable pageable
    );

    @Modifying
    @Query("delete from KbEmbeddingIndexTask task where task.versionId = :versionId")
    int deleteByVersionId(@Param("versionId") Long versionId);
}
