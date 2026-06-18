package com.sunxin.knowledge.task;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sunxin.knowledge.auth.AccessControlService;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.CurrentUserResolver;
import com.sunxin.knowledge.auth.PermissionAction;
import com.sunxin.knowledge.common.api.ApiResponse;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.knowledgebase.application.KnowledgeSpaceApplicationService;
import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.persistence.entity.KbEmbeddingIndexTask;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbDocumentParseTaskRepository;
import com.sunxin.knowledge.persistence.repository.KbEmbeddingIndexTaskRepository;
import com.sunxin.knowledge.task.dto.EmbeddingIndexTaskResponse;
import com.sunxin.knowledge.task.dto.ParseTaskResponse;
import com.sunxin.knowledge.task.dto.UnifiedTaskResponse;

import jakarta.validation.constraints.NotNull;

@Validated
@RestController
public class TaskController {

    private final DocumentParseTaskExecutionService executionService;
    private final EmbeddingIndexTaskExecutionService embeddingExecutionService;
    private final TaskCenterService taskCenterService;
    private final KbDocumentParseTaskRepository taskRepository;
    private final KbEmbeddingIndexTaskRepository embeddingTaskRepository;
    private final KnowledgeSpaceApplicationService spaceService;
    private final AccessControlService accessControlService;
    private final CurrentUserResolver currentUserResolver;

    public TaskController(
            DocumentParseTaskExecutionService executionService,
            EmbeddingIndexTaskExecutionService embeddingExecutionService,
            TaskCenterService taskCenterService,
            KbDocumentParseTaskRepository taskRepository,
            KbEmbeddingIndexTaskRepository embeddingTaskRepository,
            KnowledgeSpaceApplicationService spaceService,
            AccessControlService accessControlService,
            CurrentUserResolver currentUserResolver
    ) {
        this.executionService = executionService;
        this.embeddingExecutionService = embeddingExecutionService;
        this.taskCenterService = taskCenterService;
        this.taskRepository = taskRepository;
        this.embeddingTaskRepository = embeddingTaskRepository;
        this.spaceService = spaceService;
        this.accessControlService = accessControlService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/api/v1/tasks/center")
    public ApiResponse<List<UnifiedTaskResponse>> listUnifiedTasks(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam @NotNull Long spaceId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String taskCategory,
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        KbSpace space = spaceService.requireActiveSpace(spaceId);
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId, space.getTenantId());
        accessControlService.requireSpacePermission(space, currentUser, PermissionAction.SPACE_READ);
        return ApiResponse.ok(taskCenterService.list(spaceId, status, taskCategory, limit));
    }

    @GetMapping("/api/v1/tasks/parse")
    public ApiResponse<List<ParseTaskResponse>> listParseTasks(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam @NotNull Long spaceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        KbSpace space = spaceService.requireActiveSpace(spaceId);
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId, space.getTenantId());
        accessControlService.requireSpacePermission(space, currentUser, PermissionAction.SPACE_READ);
        return ApiResponse.ok(executionService.list(spaceId, status, limit == null ? 50 : limit));
    }

    @GetMapping("/api/v1/tasks/embedding")
    public ApiResponse<List<EmbeddingIndexTaskResponse>> listEmbeddingTasks(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @RequestParam @NotNull Long spaceId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") Integer limit
    ) {
        KbSpace space = spaceService.requireActiveSpace(spaceId);
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId, space.getTenantId());
        accessControlService.requireSpacePermission(space, currentUser, PermissionAction.SPACE_READ);
        return ApiResponse.ok(embeddingExecutionService.list(spaceId, status, limit == null ? 50 : limit));
    }

    @PostMapping("/api/v1/tasks/center/{taskKey}/run")
    public ApiResponse<UnifiedTaskResponse> runUnifiedTask(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String taskKey
    ) {
        requireUnifiedTaskPermission(taskKey, userId, tenantId, PermissionAction.DOCUMENT_UPLOAD);
        return ApiResponse.ok(taskCenterService.run(taskKey));
    }

    @PostMapping("/api/v1/tasks/{taskId}/run")
    public ApiResponse<ParseTaskResponse> runParseTask(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable @NotNull Long taskId
    ) {
        requireTaskPermission(taskId, userId, tenantId, PermissionAction.DOCUMENT_UPLOAD);
        return ApiResponse.ok(executionService.process(taskId));
    }

    @PostMapping("/api/v1/tasks/embedding/{taskId}/run")
    public ApiResponse<EmbeddingIndexTaskResponse> runEmbeddingTask(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable @NotNull Long taskId
    ) {
        requireEmbeddingTaskPermission(taskId, userId, tenantId, PermissionAction.DOCUMENT_UPLOAD);
        return ApiResponse.ok(embeddingExecutionService.process(taskId));
    }

    @PostMapping("/api/v1/tasks/center/{taskKey}/retry")
    public ApiResponse<UnifiedTaskResponse> retryUnifiedTask(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable String taskKey
    ) {
        requireUnifiedTaskPermission(taskKey, userId, tenantId, PermissionAction.DOCUMENT_UPLOAD);
        return ApiResponse.ok(taskCenterService.retry(taskKey));
    }

    @PostMapping("/api/v1/tasks/{taskId}/retry")
    public ApiResponse<ParseTaskResponse> retryParseTask(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable @NotNull Long taskId
    ) {
        requireTaskPermission(taskId, userId, tenantId, PermissionAction.DOCUMENT_UPLOAD);
        return ApiResponse.ok(executionService.retry(taskId));
    }

    @PostMapping("/api/v1/tasks/embedding/{taskId}/retry")
    public ApiResponse<EmbeddingIndexTaskResponse> retryEmbeddingTask(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-Tenant-Id", required = false) Long tenantId,
            @PathVariable @NotNull Long taskId
    ) {
        requireEmbeddingTaskPermission(taskId, userId, tenantId, PermissionAction.DOCUMENT_UPLOAD);
        return ApiResponse.ok(embeddingExecutionService.retry(taskId));
    }

    private void requireTaskPermission(Long taskId, Long userId, Long tenantId, String action) {
        KbDocumentParseTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Document parse task not found"));
        KbSpace space = spaceService.requireActiveSpace(task.getSpaceId());
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId, task.getTenantId());
        accessControlService.requireSpacePermission(space, currentUser, action);
    }

    private void requireUnifiedTaskPermission(String taskKey, Long userId, Long tenantId, String action) {
        TaskCenterService.TaskScope scope = taskCenterService.requireScope(taskKey);
        KbSpace space = spaceService.requireActiveSpace(scope.spaceId());
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId, scope.tenantId());
        accessControlService.requireSpacePermission(space, currentUser, action);
    }

    private void requireEmbeddingTaskPermission(Long taskId, Long userId, Long tenantId, String action) {
        KbEmbeddingIndexTask task = embeddingTaskRepository.findById(taskId)
                .orElseThrow(() -> new NotFoundException("Embedding index task not found"));
        KbSpace space = spaceService.requireActiveSpace(task.getSpaceId());
        CurrentUser currentUser = currentUserResolver.resolve(userId, tenantId, task.getTenantId());
        accessControlService.requireSpacePermission(space, currentUser, action);
    }
}
