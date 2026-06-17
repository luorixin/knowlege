package com.sunxin.knowledge.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "kb_document_version")
public class KbDocumentVersion extends AuditableEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "doc_id", nullable = false)
    private Long docId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "source_uri", length = 1024)
    private String sourceUri;

    @Column(name = "storage_uri", length = 1024)
    private String storageUri;

    @Column(name = "file_hash", nullable = false, length = 128)
    private String fileHash;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "parser_profile", length = 128)
    private String parserProfile;

    @Column(name = "parse_status", nullable = false, length = 32)
    private String parseStatus = "PENDING";

    @Column(name = "desensitize_status", nullable = false, length = 32)
    private String desensitizeStatus = "PENDING";

    @Column(name = "desensitized_at")
    private LocalDateTime desensitizedAt;

    @Column(name = "chunk_count", nullable = false)
    private Integer chunkCount = 0;

    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens = 0;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "ACTIVE";

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(Long spaceId) {
        this.spaceId = spaceId;
    }

    public Long getDocId() {
        return docId;
    }

    public void setDocId(Long docId) {
        this.docId = docId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public String getStorageUri() {
        return storageUri;
    }

    public void setStorageUri(String storageUri) {
        this.storageUri = storageUri;
    }

    public String getFileHash() {
        return fileHash;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getParserProfile() {
        return parserProfile;
    }

    public void setParserProfile(String parserProfile) {
        this.parserProfile = parserProfile;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getDesensitizeStatus() {
        return desensitizeStatus;
    }

    public void setDesensitizeStatus(String desensitizeStatus) {
        this.desensitizeStatus = desensitizeStatus;
    }

    public LocalDateTime getDesensitizedAt() {
        return desensitizedAt;
    }

    public void setDesensitizedAt(LocalDateTime desensitizedAt) {
        this.desensitizedAt = desensitizedAt;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
