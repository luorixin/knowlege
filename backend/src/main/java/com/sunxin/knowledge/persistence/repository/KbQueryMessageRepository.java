package com.sunxin.knowledge.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbQueryMessage;

public interface KbQueryMessageRepository extends JpaRepository<KbQueryMessage, Long> {

    List<KbQueryMessage> findBySessionIdOrderByCreatedAt(Long sessionId);
}
