package com.sunxin.knowledge.persistence.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbQueryMessage;

public interface KbQueryMessageRepository extends JpaRepository<KbQueryMessage, Long> {

    Page<KbQueryMessage> findBySessionIdOrderByCreatedAt(Long sessionId, Pageable pageable);
    
    List<KbQueryMessage> findBySessionIdOrderByCreatedAt(Long sessionId);

    Page<KbQueryMessage> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);
}
