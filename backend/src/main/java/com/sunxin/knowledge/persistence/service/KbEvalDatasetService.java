package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbEvalDataset;
import com.sunxin.knowledge.persistence.repository.KbEvalDatasetRepository;

@Service
public class KbEvalDatasetService extends BaseCrudService<KbEvalDataset> {

    public KbEvalDatasetService(KbEvalDatasetRepository repository) {
        super(repository);
    }
}
