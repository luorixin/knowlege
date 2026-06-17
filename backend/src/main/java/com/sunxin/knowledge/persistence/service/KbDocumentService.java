package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;

@Service
public class KbDocumentService extends BaseCrudService<KbDocument> {

    public KbDocumentService(KbDocumentRepository repository) {
        super(repository);
    }
}
