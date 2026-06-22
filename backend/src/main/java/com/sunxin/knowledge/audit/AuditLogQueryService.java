package com.sunxin.knowledge.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.audit.dto.AuditLogQueryRequest;
import com.sunxin.knowledge.audit.dto.AuditLogResponse;
import com.sunxin.knowledge.auth.AdminAccessService;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.common.dto.PageResponse;
import com.sunxin.knowledge.persistence.entity.KbAuditLog;
import com.sunxin.knowledge.persistence.repository.KbAuditLogRepository;

@Service
public class AuditLogQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

    private final KbAuditLogRepository auditLogRepository;
    private final AdminAccessService adminAccessService;
    private final AuditLogRecorder auditLogRecorder;

    public AuditLogQueryService(
            KbAuditLogRepository auditLogRepository,
            AdminAccessService adminAccessService,
            AuditLogRecorder auditLogRecorder
    ) {
        this.auditLogRepository = auditLogRepository;
        this.adminAccessService = adminAccessService;
        this.auditLogRecorder = auditLogRecorder;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(AuditLogQueryRequest request, CurrentUser currentUser) {
        adminAccessService.requireAdmin(currentUser);

        Page<KbAuditLog> page = auditLogRepository.findAll(
                specification(currentUser.tenantId(), request),
                PageRequest.of(page(request), size(request), Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        auditLogRecorder.record(
                currentUser,
                currentUser.tenantId(),
                "audit_log_query",
                "AUDIT_LOG",
                null,
                AuditLogRecorder.SUCCESS,
                auditLogRecorder.detail(
                        "actor_user_id", request.actorUserId(),
                        "action", request.action(),
                        "resource_type", request.resourceType(),
                        "resource_id", request.resourceId(),
                        "result_status", request.resultStatus(),
                        "trace_id", request.traceId(),
                        "page", page(request),
                        "size", size(request)
                )
        );

        Page<AuditLogResponse> mapped = page.map(AuditLogResponse::fromEntity);
        return PageResponse.of(mapped);
    }

    private static Specification<KbAuditLog> specification(Long tenantId, AuditLogQueryRequest request) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (request.actorUserId() != null) {
                predicates.add(cb.equal(root.get("actorUserId"), request.actorUserId()));
            }
            if (hasText(request.action())) {
                predicates.add(cb.equal(root.get("action"), request.action().trim()));
            }
            if (hasText(request.resourceType())) {
                predicates.add(cb.equal(root.get("resourceType"), request.resourceType().trim()));
            }
            if (hasText(request.resourceId())) {
                predicates.add(cb.equal(root.get("resourceId"), request.resourceId().trim()));
            }
            if (hasText(request.resultStatus())) {
                predicates.add(cb.equal(root.get("resultStatus"), request.resultStatus().trim()));
            }
            if (hasText(request.traceId())) {
                predicates.add(cb.equal(root.get("traceId"), request.traceId().trim()));
            }
            if (request.createdFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.createdFrom()));
            }
            if (request.createdTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.createdTo()));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static int page(AuditLogQueryRequest request) {
        Integer page = request == null ? null : request.page();
        return page == null ? DEFAULT_PAGE : Math.max(page, 0);
    }

    private static int size(AuditLogQueryRequest request) {
        Integer size = request == null ? null : request.size();
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
