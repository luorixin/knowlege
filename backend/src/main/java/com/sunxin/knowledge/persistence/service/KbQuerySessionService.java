package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbQuerySession;
import com.sunxin.knowledge.persistence.repository.KbQuerySessionRepository;

@Service
public class KbQuerySessionService extends BaseCrudService<KbQuerySession> {

    public KbQuerySessionService(KbQuerySessionRepository repository) {
        super(repository);
    }
}
