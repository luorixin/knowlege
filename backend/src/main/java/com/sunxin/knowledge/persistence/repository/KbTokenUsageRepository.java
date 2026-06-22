package com.sunxin.knowledge.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.sunxin.knowledge.persistence.entity.KbTokenUsage;

@Repository
public interface KbTokenUsageRepository extends JpaRepository<KbTokenUsage, Long>, JpaSpecificationExecutor<KbTokenUsage> {
}
