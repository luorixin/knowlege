package com.sunxin.knowledge.retrieval.security;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.auth.AccessControlService;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.retrieval.dto.SearchFilters;
import com.sunxin.knowledge.retrieval.search.ChunkSearchScope;

@Service
public class DocumentAccessFilter {

    private final AccessControlService accessControlService;

    public DocumentAccessFilter(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    public ChunkSearchScope searchScope(KbSpace space, CurrentUser user, SearchFilters filters) {
        CurrentUser resolvedUser = accessControlService.resolveForTenant(user, space.getTenantId());
        return new ChunkSearchScope(
                space.getTenantId(),
                space.getId(),
                resolvedUser.userId(),
                String.valueOf(resolvedUser.userId()),
                roleSubjects(resolvedUser.roleCodes()),
                isOwner(space, resolvedUser),
                normalizeFilter(filters == null ? null : filters.docType()),
                normalizeFilter(filters == null ? null : filters.industry()),
                normalizeFilter(filters == null ? null : filters.serviceLine()),
                createdFrom(filters == null ? null : filters.yearFrom()),
                normalizeFilter(filters == null ? null : filters.blockType()),
                normalizeFilter(filters == null ? null : filters.contentType()),
                filters == null ? null : filters.minConfidence(),
                normalizeFilter(filters == null ? null : filters.parser()),
                normalizeFilter(filters == null ? null : filters.pageParseMode())
        );
    }

    private static boolean isOwner(KbSpace space, CurrentUser user) {
        return space.getOwnerUserId() != null && space.getOwnerUserId().equals(user.userId());
    }

    private static List<String> roleSubjects(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return List.of("__no_role__");
        }
        return roleCodes.stream()
                .map(DocumentAccessFilter::normalize)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toList());
    }

    private static String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static LocalDateTime createdFrom(Integer yearFrom) {
        if (yearFrom == null) {
            return null;
        }
        return LocalDateTime.of(yearFrom, Month.JANUARY, 1, 0, 0);
    }

}
