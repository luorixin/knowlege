package com.sunxin.knowledge.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;

public interface KbDocumentChunkRepository extends JpaRepository<KbDocumentChunk, Long> {

    Optional<KbDocumentChunk> findByVersionIdAndChunkIndex(Long versionId, Integer chunkIndex);

    List<KbDocumentChunk> findByVersionIdOrderByChunkIndex(Long versionId);

    List<KbDocumentChunk> findByDocIdInAndStatus(Collection<Long> docIds, String status);

    @Query("""
            select chunk from KbDocumentChunk chunk
            where chunk.docId in :docIds
              and chunk.status = :status
              and lower(chunk.content) like lower(concat('%', :term, '%'))
            order by chunk.updatedAt desc
            """)
    List<KbDocumentChunk> searchByDocIdsAndContentLike(
            @Param("docIds") Collection<Long> docIds,
            @Param("status") String status,
            @Param("term") String term,
            Pageable pageable
    );

    @Modifying
    @Query("delete from KbDocumentChunk chunk where chunk.versionId = :versionId")
    int deleteByVersionId(@Param("versionId") Long versionId);
}
