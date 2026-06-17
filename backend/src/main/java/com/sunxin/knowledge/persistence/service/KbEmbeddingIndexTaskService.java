package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbEmbeddingIndexTask;
import com.sunxin.knowledge.persistence.repository.KbEmbeddingIndexTaskRepository;

@Service
public class KbEmbeddingIndexTaskService extends BaseCrudService<KbEmbeddingIndexTask> {

    public KbEmbeddingIndexTaskService(KbEmbeddingIndexTaskRepository repository) {
        super(repository);
    }
}
