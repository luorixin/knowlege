package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbDocumentParseTask;
import com.sunxin.knowledge.persistence.repository.KbDocumentParseTaskRepository;

@Service
public class KbDocumentParseTaskService extends BaseCrudService<KbDocumentParseTask> {

    public KbDocumentParseTaskService(KbDocumentParseTaskRepository repository) {
        super(repository);
    }
}
