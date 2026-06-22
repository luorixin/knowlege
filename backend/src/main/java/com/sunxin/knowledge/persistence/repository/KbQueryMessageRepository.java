package com.sunxin.knowledge.persistence.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbQueryMessage;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface KbQueryMessageRepository extends JpaRepository<KbQueryMessage, Long>, JpaSpecificationExecutor<KbQueryMessage> {

    Page<KbQueryMessage> findBySessionIdOrderByCreatedAt(Long sessionId, Pageable pageable);
    
    List<KbQueryMessage> findBySessionIdOrderByCreatedAt(Long sessionId);

    Page<KbQueryMessage> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);
}
