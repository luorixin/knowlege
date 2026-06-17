package com.sunxin.knowledge.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbSpace;

public interface KbSpaceRepository extends JpaRepository<KbSpace, Long> {

    List<KbSpace> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, String status);

    Optional<KbSpace> findByIdAndStatus(Long id, String status);
}
