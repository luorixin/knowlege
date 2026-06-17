package com.sunxin.knowledge.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbRole;

public interface KbRoleRepository extends JpaRepository<KbRole, Long> {

    Optional<KbRole> findByTenantIdAndCodeAndStatus(Long tenantId, String code, String status);
}
