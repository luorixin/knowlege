package com.sunxin.knowledge.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sunxin.knowledge.persistence.entity.KbAnswerCitation;

public interface KbAnswerCitationRepository extends JpaRepository<KbAnswerCitation, Long> {

    List<KbAnswerCitation> findByMessageIdOrderByRankNo(Long messageId);
    
    List<KbAnswerCitation> findByMessageIdIn(List<Long> messageIds);
}
