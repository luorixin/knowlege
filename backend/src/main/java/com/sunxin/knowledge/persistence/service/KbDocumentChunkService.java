package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;

@Service
public class KbDocumentChunkService extends BaseCrudService<KbDocumentChunk> {

    public KbDocumentChunkService(KbDocumentChunkRepository repository) {
        super(repository);
    }
}
