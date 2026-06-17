package com.sunxin.knowledge.retrieval.security;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.auth.AccessControlService;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.auth.PermissionAction;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.retrieval.dto.SearchFilters;

@Service
public class DocumentAccessFilter {

    private static final String DELETED = "DELETED";

    private final KbDocumentRepository documentRepository;
    private final AccessControlService accessControlService;

    public DocumentAccessFilter(
            KbDocumentRepository documentRepository,
            AccessControlService accessControlService
    ) {
        this.documentRepository = documentRepository;
        this.accessControlService = accessControlService;
    }

    public List<KbDocument> accessibleDocuments(KbSpace space, CurrentUser user, SearchFilters filters) {
        CurrentUser resolvedUser = accessControlService.resolveForTenant(user, space.getTenantId());
        return documentRepository
                .findBySpaceIdAndStatusNotOrderByCreatedAtDesc(space.getId(), DELETED)
                .stream()
                .filter(document -> document.getTenantId().equals(space.getTenantId()))
                .filter(document -> matchesMetadata(document, filters))
                .filter(document -> accessControlService.canAccessDocument(
                        space,
                        document,
                        resolvedUser,
                        PermissionAction.DOCUMENT_READ
                ))
                .toList();
    }

    private static boolean matchesMetadata(KbDocument document, SearchFilters filters) {
        if (filters == null) {
            return true;
        }
        return equalsIgnoreCaseIfPresent(document.getDocType(), filters.docType())
                && equalsIgnoreCaseIfPresent(document.getIndustry(), filters.industry())
                && equalsIgnoreCaseIfPresent(document.getServiceLine(), filters.serviceLine())
                && matchesYearFrom(document, filters.yearFrom());
    }

    private static boolean matchesYearFrom(KbDocument document, Integer yearFrom) {
        if (yearFrom == null) {
            return true;
        }
        if (document.getCreatedAt() == null) {
            return true;
        }
        LocalDateTime start = LocalDateTime.of(yearFrom, Month.JANUARY, 1, 0, 0);
        return !document.getCreatedAt().isBefore(start);
    }

    private static boolean equalsIgnoreCaseIfPresent(String actual, String expected) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        return actual != null && actual.equalsIgnoreCase(expected.trim());
    }

}
