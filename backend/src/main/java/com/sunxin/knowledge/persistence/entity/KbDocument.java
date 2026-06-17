package com.sunxin.knowledge.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "kb_document")
public class KbDocument extends AuditableEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "space_id", nullable = false)
    private Long spaceId;

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "doc_type", nullable = false, length = 64)
    private String docType;

    @Column(name = "industry", length = 128)
    private String industry;

    @Column(name = "service_line", length = 128)
    private String serviceLine;

    @Column(name = "confidential_level", nullable = false, length = 64)
    private String confidentialLevel = "INTERNAL";

    @Column(name = "source_uri", length = 1024)
    private String sourceUri;

    @Column(name = "storage_uri", length = 1024)
    private String storageUri;

    @Column(name = "file_hash", nullable = false, length = 128)
    private String fileHash;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "current_version_id")
    private Long currentVersionId;

    @Column(name = "status", nullable = false, length = 32)
    private String status = "UPLOADED";

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getServiceLine() {
        return serviceLine;
    }

    public void setServiceLine(String serviceLine) {
        this.serviceLine = serviceLine;
    }

    public String getConfidentialLevel() {
        return confidentialLevel;
    }

    public void setConfidentialLevel(String confidentialLevel) {
        this.confidentialLevel = confidentialLevel;
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

    public Long getCurrentVersionId() {
        return currentVersionId;
    }

    public void setCurrentVersionId(Long currentVersionId) {
        this.currentVersionId = currentVersionId;
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
