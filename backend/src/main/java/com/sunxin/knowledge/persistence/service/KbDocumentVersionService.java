package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbDocumentVersion;
import com.sunxin.knowledge.persistence.repository.KbDocumentVersionRepository;

@Service
public class KbDocumentVersionService extends BaseCrudService<KbDocumentVersion> {

    public KbDocumentVersionService(KbDocumentVersionRepository repository) {
        super(repository);
    }
}
