package com.sunxin.knowledge.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbUser;

public interface KbUserRepository extends JpaRepository<KbUser, Long> {

    Optional<KbUser> findByTenantIdAndIdAndStatus(Long tenantId, Long id, String status);

    Optional<KbUser> findByTenantIdAndUsername(Long tenantId, String username);
    
    Optional<KbUser> findFirstByUsernameAndStatus(String username, String status);
    
    List<KbUser> findByTenantIdAndStatus(Long tenantId, String status);
}
