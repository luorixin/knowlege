package com.sunxin.knowledge.document.api;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.CurrentUserResolver;
import com.sunxin.knowledge.common.api.ApiResponse;
import com.sunxin.knowledge.common.dto.PageResponse;
import com.sunxin.knowledge.document.application.DocumentChunkingService;
import com.sunxin.knowledge.document.application.DocumentDesensitizationService;
import com.sunxin.knowledge.document.application.DocumentIngestionService;
import com.sunxin.knowledge.document.dto.DesensitizationMappingResponse;
import com.sunxin.knowledge.document.dto.DocumentDeleteResponse;
import com.sunxin.knowledge.document.dto.DocumentDetailResponse;
import com.sunxin.knowledge.document.dto.DocumentListItemResponse;
import com.sunxin.knowledge.document.dto.DocumentParseStatusResponse;
import com.sunxin.knowledge.document.dto.DocumentUploadRequest;
import com.sunxin.knowledge.document.dto.DocumentUploadResponse;
import com.sunxin.knowledge.document.dto.RebuildChunksRequest;
import com.sunxin.knowledge.document.dto.RebuildChunksResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.web.bind.annotation.RequestMapping;

@Validated
@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentIngestionService documentService;
    private final DocumentChunkingService chunkingService;
    private final DocumentDesensitizationService desensitizationService;
    private final CurrentUserResolver currentUserResolver;

    public DocumentController(
            DocumentIngestionService documentService,
            DocumentChunkingService chunkingService,
            DocumentDesensitizationService desensitizationService,
            CurrentUserResolver currentUserResolver
    ) {
        this.documentService = documentService;
        this.chunkingService = chunkingService;
        this.desensitizationService = desensitizationService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping(
            value = "/kb-spaces/{spaceId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<DocumentUploadResponse> upload(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable Long spaceId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "industry", required = false) String industry,
            @RequestParam(value = "serviceLine", required = false) String serviceLine,
            @RequestParam(value = "confidentialLevel", required = false) String confidentialLevel
    ) {
        DocumentUploadRequest request = new DocumentUploadRequest(
                title,
                industry,
                serviceLine,
                confidentialLevel
        );
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(documentService.upload(spaceId, file, request, currentUser));
    }

    @GetMapping("/kb-spaces/{spaceId}/documents")
    public ApiResponse<PageResponse<DocumentListItemResponse>> listBySpace(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable @NotNull Long spaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(documentService.listBySpace(spaceId, page, size, currentUser));
    }

    @GetMapping("/documents/{documentId}")
    public ApiResponse<DocumentDetailResponse> detail(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable @NotNull Long documentId
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(documentService.detail(documentId, currentUser));
    }

    @DeleteMapping("/documents/{documentId}")
    public ApiResponse<DocumentDeleteResponse> delete(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable @NotNull Long documentId
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(documentService.delete(documentId, currentUser));
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<Resource> download(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable @NotNull Long documentId
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        Resource file = documentService.download(documentId, currentUser);
        DocumentDetailResponse detail = documentService.detail(documentId, currentUser);
        
        String filename = detail.title();
        // Simple sanitization for Content-Disposition header
        filename = filename != null ? filename.replaceAll("[\"\\\\]", "") : "document";
        
        if (detail.sourceUri() != null) {
            int lastDot = detail.sourceUri().lastIndexOf('.');
            if (lastDot > 0 && lastDot > detail.sourceUri().lastIndexOf('/')) {
                String ext = detail.sourceUri().substring(lastDot);
                if (!filename.toLowerCase().endsWith(ext.toLowerCase())) {
                    filename += ext;
                }
            }
        }
        
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        
        org.springframework.http.MediaType mediaType = org.springframework.http.MediaTypeFactory.getMediaType(filename).orElse(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
        if (mediaType.getType().equals("text") && mediaType.getCharset() == null) {
            mediaType = new org.springframework.http.MediaType(mediaType, java.nio.charset.StandardCharsets.UTF_8);
        }
        
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename)
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(file);
    }

    @GetMapping("/documents/{documentId}/parse-status")
    public ApiResponse<DocumentParseStatusResponse> parseStatus(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable @NotNull Long documentId
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(documentService.parseStatus(documentId, currentUser));
    }

    @PostMapping("/documents/{documentId}/chunks/rebuild")
    public ApiResponse<RebuildChunksResponse> rebuildChunks(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable @NotNull Long documentId,
            @Valid @RequestBody RebuildChunksRequest request
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(chunkingService.rebuildChunks(documentId, request, currentUser));
    }

    @GetMapping("/documents/{documentId}/chunks")
    public ApiResponse<List<com.sunxin.knowledge.document.dto.ChunkResponse>> listChunks(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable @NotNull Long documentId
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(chunkingService.listChunks(documentId, currentUser));
    }

    @GetMapping("/documents/{documentId}/desensitization-mappings")
    public ApiResponse<List<DesensitizationMappingResponse>> desensitizationMappings(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable @NotNull Long documentId
    ) {
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId);
        return ApiResponse.ok(desensitizationService.mappings(documentId, currentUser));
    }
}
