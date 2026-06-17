package com.sunxin.knowledge.persistence.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.sunxin.knowledge.persistence.entity.AuditableEntity;

public abstract class BaseCrudService<T extends AuditableEntity> {

    private final JpaRepository<T, Long> repository;

    protected BaseCrudService(JpaRepository<T, Long> repository) {
        this.repository = repository;
    }

    @Transactional
    public T save(T entity) {
        return repository.save(entity);
    }

    @Transactional(readOnly = true)
    public Optional<T> findById(Long id) {
        return repository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<T> findAll() {
        return repository.findAll();
    }

    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
