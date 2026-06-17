package com.sunxin.knowledge.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbDesensitizationMapping;

public interface KbDesensitizationMappingRepository extends JpaRepository<KbDesensitizationMapping, Long> {

    void deleteByVersionId(Long versionId);

    List<KbDesensitizationMapping> findByDocIdAndVersionIdAndStatusOrderByOccurrenceIndex(
            Long docId,
            Long versionId,
            String status
    );
}
