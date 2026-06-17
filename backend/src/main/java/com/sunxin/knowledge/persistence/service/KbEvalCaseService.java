package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbEvalCase;
import com.sunxin.knowledge.persistence.repository.KbEvalCaseRepository;

@Service
public class KbEvalCaseService extends BaseCrudService<KbEvalCase> {

    public KbEvalCaseService(KbEvalCaseRepository repository) {
        super(repository);
    }
}
