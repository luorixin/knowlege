package com.sunxin.knowledge.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbPermissionPolicy;

public interface KbPermissionPolicyRepository extends JpaRepository<KbPermissionPolicy, Long> {

    List<KbPermissionPolicy> findByTenantIdAndStatus(Long tenantId, String status);
}
