package com.cloudconfig.smart.pay.hub.sph.repository;

import com.cloudconfig.smart.pay.hub.sph.entity.ConfigurationAuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConfigurationAuditRepository extends JpaRepository<ConfigurationAuditEntity, String> {
    
    List<ConfigurationAuditEntity> findByConfigIdOrderByModifiedAtDesc(String configId);
    
    Page<ConfigurationAuditEntity> findByConfigId(String configId, Pageable pageable);
}