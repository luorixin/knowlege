package com.sunxin.knowledge.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.audit.dto.TokenUsageQueryRequest;
import com.sunxin.knowledge.audit.dto.TokenUsageResponse;
import com.sunxin.knowledge.auth.AdminAccessService;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.common.dto.PageResponse;
import com.sunxin.knowledge.persistence.entity.KbTokenUsage;
import com.sunxin.knowledge.persistence.repository.KbTokenUsageRepository;

@Service
public class TokenUsageQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

    private final KbTokenUsageRepository tokenUsageRepository;
    private final AdminAccessService adminAccessService;

    public TokenUsageQueryService(
            KbTokenUsageRepository tokenUsageRepository,
            AdminAccessService adminAccessService
    ) {
        this.tokenUsageRepository = tokenUsageRepository;
        this.adminAccessService = adminAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponse<TokenUsageResponse> search(TokenUsageQueryRequest request, CurrentUser currentUser) {
        adminAccessService.requireAdmin(currentUser);

        Page<KbTokenUsage> page = tokenUsageRepository.findAll(
                specification(currentUser.tenantId(), request),
                PageRequest.of(page(request), size(request), Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        Page<TokenUsageResponse> mapped = page.map(TokenUsageResponse::fromEntity);
        return PageResponse.of(mapped);
    }

    private static Specification<KbTokenUsage> specification(Long tenantId, TokenUsageQueryRequest request) {
        return (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.isNotNull(root.get("modelName")));
            
            if (request.modelProvider() != null && !request.modelProvider().isBlank()) {
                predicates.add(cb.equal(root.get("modelProvider"), request.modelProvider().trim()));
            }
            if (request.modelName() != null && !request.modelName().isBlank()) {
                predicates.add(cb.equal(root.get("modelName"), request.modelName().trim()));
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

    private static int page(TokenUsageQueryRequest request) {
        Integer page = request == null ? null : request.page();
        return page == null ? DEFAULT_PAGE : Math.max(page, 0);
    }

    private static int size(TokenUsageQueryRequest request) {
        Integer size = request == null ? null : request.size();
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(Math.max(size, 1), MAX_SIZE);
    }
}
