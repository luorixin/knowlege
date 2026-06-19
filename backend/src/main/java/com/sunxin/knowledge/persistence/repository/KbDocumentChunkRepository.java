package com.sunxin.knowledge.persistence.repository;

import java.time.LocalDateTime;
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

    List<KbDocumentChunk> findByDocId(Long docId);

    @Query("""
            select chunk from KbDocumentChunk chunk
            join KbDocument document on document.id = chunk.docId
            where chunk.tenantId = :tenantId
              and chunk.spaceId = :spaceId
              and chunk.status = :chunkStatus
              and document.tenantId = :tenantId
              and document.spaceId = :spaceId
              and document.status <> :deletedDocumentStatus
              and (cast(:docType as string) is null or lower(document.docType) = :docType)
              and (cast(:industry as string) is null or lower(document.industry) = :industry)
              and (cast(:serviceLine as string) is null or lower(document.serviceLine) = :serviceLine)
              and (cast(:createdFrom as timestamp) is null or document.createdAt is null or document.createdAt >= :createdFrom)
              and (
                    :term1 is not null and lower(chunk.content) like lower(concat('%', cast(:term1 as string), '%'))
                 or :term2 is not null and lower(chunk.content) like lower(concat('%', cast(:term2 as string), '%'))
                 or :term3 is not null and lower(chunk.content) like lower(concat('%', cast(:term3 as string), '%'))
              )
              and (
                    :spaceOwner = true
                 or (
                        exists (
                            select allowPolicy.id from KbPermissionPolicy allowPolicy
                            where allowPolicy.tenantId = :tenantId
                              and allowPolicy.status = :policyStatus
                              and lower(allowPolicy.effect) = 'allow'
                              and (allowPolicy.validFrom is null or allowPolicy.validFrom <= CURRENT_TIMESTAMP)
                              and (allowPolicy.validTo is null or allowPolicy.validTo > CURRENT_TIMESTAMP)
                              and (
                                    lower(allowPolicy.subjectType) in ('*', 'all')
                                 or lower(allowPolicy.subjectId) = '*'
                                 or (lower(allowPolicy.subjectType) = 'user' and allowPolicy.subjectId = :userSubjectId)
                                 or (lower(allowPolicy.subjectType) = 'role' and lower(allowPolicy.subjectId) in :roleSubjects)
                              )
                              and (
                                    allowPolicy.actions = '*'
                                 or lower(allowPolicy.actions) like '%admin_manage%'
                                 or lower(allowPolicy.actions) like '%admin.manage%'
                                 or lower(allowPolicy.actions) like '%admin-manage%'
                                 or lower(allowPolicy.actions) like '%document_read%'
                                 or lower(allowPolicy.actions) like '%document.read%'
                                 or lower(allowPolicy.actions) like '%document-read%'
                                 or lower(allowPolicy.actions) like '%retrieve%'
                                 or lower(allowPolicy.actions) like '%search%'
                                 or lower(allowPolicy.actions) like '%read%'
                              )
                              and (
                                    lower(allowPolicy.resourceType) in ('*', 'all')
                                 or (
                                        lower(allowPolicy.resourceType) in ('space', 'kb_space')
                                    and (allowPolicy.resourceId is null or allowPolicy.resourceId = document.spaceId)
                                 )
                                 or (
                                        lower(allowPolicy.resourceType) in ('document', 'doc', 'kb_document')
                                    and (allowPolicy.resourceId is null or allowPolicy.resourceId = document.id)
                                 )
                              )
                        )
                    and not exists (
                            select denyPolicy.id from KbPermissionPolicy denyPolicy
                            where denyPolicy.tenantId = :tenantId
                              and denyPolicy.status = :policyStatus
                              and lower(denyPolicy.effect) = 'deny'
                              and (denyPolicy.validFrom is null or denyPolicy.validFrom <= CURRENT_TIMESTAMP)
                              and (denyPolicy.validTo is null or denyPolicy.validTo > CURRENT_TIMESTAMP)
                              and (
                                    lower(denyPolicy.subjectType) in ('*', 'all')
                                 or lower(denyPolicy.subjectId) = '*'
                                 or (lower(denyPolicy.subjectType) = 'user' and denyPolicy.subjectId = :userSubjectId)
                                 or (lower(denyPolicy.subjectType) = 'role' and lower(denyPolicy.subjectId) in :roleSubjects)
                              )
                              and (
                                    denyPolicy.actions = '*'
                                 or lower(denyPolicy.actions) like '%admin_manage%'
                                 or lower(denyPolicy.actions) like '%admin.manage%'
                                 or lower(denyPolicy.actions) like '%admin-manage%'
                                 or lower(denyPolicy.actions) like '%document_read%'
                                 or lower(denyPolicy.actions) like '%document.read%'
                                 or lower(denyPolicy.actions) like '%document-read%'
                                 or lower(denyPolicy.actions) like '%retrieve%'
                                 or lower(denyPolicy.actions) like '%search%'
                                 or lower(denyPolicy.actions) like '%read%'
                              )
                              and (
                                    lower(denyPolicy.resourceType) in ('*', 'all')
                                 or (
                                        lower(denyPolicy.resourceType) in ('space', 'kb_space')
                                    and (denyPolicy.resourceId is null or denyPolicy.resourceId = document.spaceId)
                                 )
                                 or (
                                        lower(denyPolicy.resourceType) in ('document', 'doc', 'kb_document')
                                    and (denyPolicy.resourceId is null or denyPolicy.resourceId = document.id)
                                 )
                              )
                        )
                    )
              )
            order by chunk.updatedAt desc
            """)
    List<KbDocumentChunk> searchAccessibleChunksByAnyTerm(
            @Param("tenantId") Long tenantId,
            @Param("spaceId") Long spaceId,
            @Param("userSubjectId") String userSubjectId,
            @Param("roleSubjects") Collection<String> roleSubjects,
            @Param("spaceOwner") boolean spaceOwner,
            @Param("docType") String docType,
            @Param("industry") String industry,
            @Param("serviceLine") String serviceLine,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("chunkStatus") String chunkStatus,
            @Param("deletedDocumentStatus") String deletedDocumentStatus,
            @Param("policyStatus") String policyStatus,
            @Param("term1") String term1,
            @Param("term2") String term2,
            @Param("term3") String term3,
            Pageable pageable
    );

    @Query("""
            select chunk from KbDocumentChunk chunk
            join KbDocument document on document.id = chunk.docId
            where chunk.tenantId = :tenantId
              and chunk.spaceId = :spaceId
              and chunk.status = :chunkStatus
              and document.tenantId = :tenantId
              and document.spaceId = :spaceId
              and document.status <> :deletedDocumentStatus
              and (cast(:docType as string) is null or lower(document.docType) = :docType)
              and (cast(:industry as string) is null or lower(document.industry) = :industry)
              and (cast(:serviceLine as string) is null or lower(document.serviceLine) = :serviceLine)
              and (cast(:createdFrom as timestamp) is null or document.createdAt is null or document.createdAt >= :createdFrom)
              and (
                    :spaceOwner = true
                 or (
                        exists (
                            select allowPolicy.id from KbPermissionPolicy allowPolicy
                            where allowPolicy.tenantId = :tenantId
                              and allowPolicy.status = :policyStatus
                              and lower(allowPolicy.effect) = 'allow'
                              and (allowPolicy.validFrom is null or allowPolicy.validFrom <= CURRENT_TIMESTAMP)
                              and (allowPolicy.validTo is null or allowPolicy.validTo > CURRENT_TIMESTAMP)
                              and (
                                    lower(allowPolicy.subjectType) in ('*', 'all')
                                 or lower(allowPolicy.subjectId) = '*'
                                 or (lower(allowPolicy.subjectType) = 'user' and allowPolicy.subjectId = :userSubjectId)
                                 or (lower(allowPolicy.subjectType) = 'role' and lower(allowPolicy.subjectId) in :roleSubjects)
                              )
                              and (
                                    allowPolicy.actions = '*'
                                 or lower(allowPolicy.actions) like '%admin_manage%'
                                 or lower(allowPolicy.actions) like '%admin.manage%'
                                 or lower(allowPolicy.actions) like '%admin-manage%'
                                 or lower(allowPolicy.actions) like '%document_read%'
                                 or lower(allowPolicy.actions) like '%document.read%'
                                 or lower(allowPolicy.actions) like '%document-read%'
                                 or lower(allowPolicy.actions) like '%retrieve%'
                                 or lower(allowPolicy.actions) like '%search%'
                                 or lower(allowPolicy.actions) like '%read%'
                              )
                              and (
                                    lower(allowPolicy.resourceType) in ('*', 'all')
                                 or (
                                        lower(allowPolicy.resourceType) in ('space', 'kb_space')
                                    and (allowPolicy.resourceId is null or allowPolicy.resourceId = document.spaceId)
                                 )
                                 or (
                                        lower(allowPolicy.resourceType) in ('document', 'doc', 'kb_document')
                                    and (allowPolicy.resourceId is null or allowPolicy.resourceId = document.id)
                                 )
                              )
                        )
                    and not exists (
                            select denyPolicy.id from KbPermissionPolicy denyPolicy
                            where denyPolicy.tenantId = :tenantId
                              and denyPolicy.status = :policyStatus
                              and lower(denyPolicy.effect) = 'deny'
                              and (denyPolicy.validFrom is null or denyPolicy.validFrom <= CURRENT_TIMESTAMP)
                              and (denyPolicy.validTo is null or denyPolicy.validTo > CURRENT_TIMESTAMP)
                              and (
                                    lower(denyPolicy.subjectType) in ('*', 'all')
                                 or lower(denyPolicy.subjectId) = '*'
                                 or (lower(denyPolicy.subjectType) = 'user' and denyPolicy.subjectId = :userSubjectId)
                                 or (lower(denyPolicy.subjectType) = 'role' and lower(denyPolicy.subjectId) in :roleSubjects)
                              )
                              and (
                                    denyPolicy.actions = '*'
                                 or lower(denyPolicy.actions) like '%admin_manage%'
                                 or lower(denyPolicy.actions) like '%admin.manage%'
                                 or lower(denyPolicy.actions) like '%admin-manage%'
                                 or lower(denyPolicy.actions) like '%document_read%'
                                 or lower(denyPolicy.actions) like '%document.read%'
                                 or lower(denyPolicy.actions) like '%document-read%'
                                 or lower(denyPolicy.actions) like '%retrieve%'
                                 or lower(denyPolicy.actions) like '%search%'
                                 or lower(denyPolicy.actions) like '%read%'
                              )
                              and (
                                    lower(denyPolicy.resourceType) in ('*', 'all')
                                 or (
                                        lower(denyPolicy.resourceType) in ('space', 'kb_space')
                                    and (denyPolicy.resourceId is null or denyPolicy.resourceId = document.spaceId)
                                 )
                                 or (
                                        lower(denyPolicy.resourceType) in ('document', 'doc', 'kb_document')
                                    and (denyPolicy.resourceId is null or denyPolicy.resourceId = document.id)
                                 )
                              )
                        )
                    )
              )
            order by chunk.updatedAt desc
            """)
    List<KbDocumentChunk> findAccessibleChunksForMockVector(
            @Param("tenantId") Long tenantId,
            @Param("spaceId") Long spaceId,
            @Param("userSubjectId") String userSubjectId,
            @Param("roleSubjects") Collection<String> roleSubjects,
            @Param("spaceOwner") boolean spaceOwner,
            @Param("docType") String docType,
            @Param("industry") String industry,
            @Param("serviceLine") String serviceLine,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("chunkStatus") String chunkStatus,
            @Param("deletedDocumentStatus") String deletedDocumentStatus,
            @Param("policyStatus") String policyStatus,
            Pageable pageable
    );

    @Modifying
    @Query("delete from KbDocumentChunk chunk where chunk.versionId = :versionId")
    int deleteByVersionId(@Param("versionId") Long versionId);
}
