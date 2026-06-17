package com.sunxin.knowledge.persistence.repository;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sunxin.knowledge.persistence.entity.KbUserRole;

public interface KbUserRoleRepository extends JpaRepository<KbUserRole, Long> {

    @Query("""
            select r.code
            from KbUserRole ur
            join KbRole r on r.id = ur.roleId and r.tenantId = ur.tenantId
            where ur.tenantId = :tenantId
              and ur.userId = :userId
              and ur.status = 'ACTIVE'
              and r.status = 'ACTIVE'
            """)
    Set<String> findActiveRoleCodes(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId
    );

    @Query("""
            select cast(r.id as string)
            from KbUserRole ur
            join KbRole r on r.id = ur.roleId and r.tenantId = ur.tenantId
            where ur.tenantId = :tenantId
              and ur.userId = :userId
              and ur.status = 'ACTIVE'
              and r.status = 'ACTIVE'
            """)
    Set<String> findActiveRoleIds(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId
    );
}
