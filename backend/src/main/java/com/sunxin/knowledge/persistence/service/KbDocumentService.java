package com.sunxin.knowledge.persistence.service;

import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;

@Service
public class KbDocumentService extends BaseCrudService<KbDocument> {

    public KbDocumentService(KbDocumentRepository repository) {
        super(repository);
    }

    @Override
    @Cacheable(value = "documents", key = "#id")
    public Optional<KbDocument> findById(Long id) {
        return super.findById(id);
    }

    @Override
    @CacheEvict(value = "documents", key = "#entity.id")
    public KbDocument save(KbDocument entity) {
        return super.save(entity);
    }

    @Override
    @CacheEvict(value = "documents", key = "#id")
    public void deleteById(Long id) {
        super.deleteById(id);
    }
}
