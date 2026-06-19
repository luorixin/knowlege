package com.sunxin.knowledge.persistence.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbQuerySession;

public interface KbQuerySessionRepository extends JpaRepository<KbQuerySession, Long> {

    Optional<KbQuerySession> findByIdAndTenantIdAndUserIdAndStatus(
            Long id,
            Long tenantId,
            Long userId,
            String status
    );

    Page<KbQuerySession> findByTenantIdAndSpaceIdAndUserIdAndStatusOrderByCreatedAtDesc(
            Long tenantId,
            Long spaceId,
            Long userId,
            String status,
            Pageable pageable
    );
}
