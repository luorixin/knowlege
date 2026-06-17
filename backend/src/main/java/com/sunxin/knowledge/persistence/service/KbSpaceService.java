package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;

@Service
public class KbSpaceService extends BaseCrudService<KbSpace> {

    public KbSpaceService(KbSpaceRepository repository) {
        super(repository);
    }
}
