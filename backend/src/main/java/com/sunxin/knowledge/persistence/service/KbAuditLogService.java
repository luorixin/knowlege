package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbAuditLog;
import com.sunxin.knowledge.persistence.repository.KbAuditLogRepository;

@Service
public class KbAuditLogService extends BaseCrudService<KbAuditLog> {

    public KbAuditLogService(KbAuditLogRepository repository) {
        super(repository);
    }
}
