package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbQueryMessage;
import com.sunxin.knowledge.persistence.repository.KbQueryMessageRepository;

@Service
public class KbQueryMessageService extends BaseCrudService<KbQueryMessage> {

    public KbQueryMessageService(KbQueryMessageRepository repository) {
        super(repository);
    }
}
