package com.sunxin.knowledge.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbSpace;

public interface KbSpaceRepository extends JpaRepository<KbSpace, Long> {

    Page<KbSpace> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, String status, Pageable pageable);

    Optional<KbSpace> findByIdAndStatus(Long id, String status);
}
