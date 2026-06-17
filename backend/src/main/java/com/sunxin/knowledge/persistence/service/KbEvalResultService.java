package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbEvalResult;
import com.sunxin.knowledge.persistence.repository.KbEvalResultRepository;

@Service
public class KbEvalResultService extends BaseCrudService<KbEvalResult> {

    public KbEvalResultService(KbEvalResultRepository repository) {
        super(repository);
    }
}
