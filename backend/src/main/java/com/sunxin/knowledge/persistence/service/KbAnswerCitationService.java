package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbAnswerCitation;
import com.sunxin.knowledge.persistence.repository.KbAnswerCitationRepository;

@Service
public class KbAnswerCitationService extends BaseCrudService<KbAnswerCitation> {

    public KbAnswerCitationService(KbAnswerCitationRepository repository) {
        super(repository);
    }
}
