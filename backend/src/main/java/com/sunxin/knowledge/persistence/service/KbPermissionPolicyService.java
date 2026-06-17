package com.sunxin.knowledge.persistence.service;

import org.springframework.stereotype.Service;

import com.sunxin.knowledge.persistence.entity.KbPermissionPolicy;
import com.sunxin.knowledge.persistence.repository.KbPermissionPolicyRepository;

@Service
public class KbPermissionPolicyService extends BaseCrudService<KbPermissionPolicy> {

    public KbPermissionPolicyService(KbPermissionPolicyRepository repository) {
        super(repository);
    }
}
