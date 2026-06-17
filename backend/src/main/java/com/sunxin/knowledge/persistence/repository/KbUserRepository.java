package com.sunxin.knowledge.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbUser;

public interface KbUserRepository extends JpaRepository<KbUser, Long> {

    Optional<KbUser> findByTenantIdAndIdAndStatus(Long tenantId, Long id, String status);
}
