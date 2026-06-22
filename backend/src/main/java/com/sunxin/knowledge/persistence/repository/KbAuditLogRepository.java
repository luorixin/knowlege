package com.sunxin.knowledge.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.sunxin.knowledge.persistence.entity.KbAuditLog;

public interface KbAuditLogRepository extends JpaRepository<KbAuditLog, Long>, JpaSpecificationExecutor<KbAuditLog> {
}
